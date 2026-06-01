"""Edge task simulator — runs inside each Docker container."""
import json
import time
import random
import os
from http.server import HTTPServer, BaseHTTPRequestHandler

NODE_TYPE = os.environ.get("NODE_TYPE", "edge")  # cloud / edge / device
MIPS = int(os.environ.get("MIPS", "1000"))        # compute power

class Handler(BaseHTTPRequestHandler):
    def do_POST(self):
        if self.path == "/execute":
            length = int(self.headers.get("Content-Length", 0))
            body = json.loads(self.rfile.read(length))
            task_type = body.get("taskType", "extract")
            workload = body.get("workload", 100)

            # Simulate compute: workload / MIPS * base_time
            base_cost = {"preprocess": 0.3, "extract": 1.0, "protect": 0.15, "match": 0.5}
            factor = base_cost.get(task_type, 1.0)
            compute_ms = (workload / MIPS) * factor * 5000
            noise = random.gauss(0, compute_ms * 0.05)
            latency = max(5, int(compute_ms + noise))

            # Simulate energy: static + dynamic
            static_power = {"cloud": 200, "edge": 30, "device": 5}[NODE_TYPE]
            dynamic_power = static_power * 0.8
            energy_mj = (static_power * 10 + dynamic_power * latency / 1000.0)

            time.sleep(min(latency / 1000.0, 10.0))  # cap at 10s

            self.send_response(200)
            self.send_header("Content-Type", "application/json")
            self.end_headers()
            self.wfile.write(json.dumps({
                "status": "success",
                "nodeType": NODE_TYPE,
                "taskType": task_type,
                "latencyMs": latency,
                "energyMj": round(energy_mj, 2),
            }).encode())

        elif self.path == "/health":
            self.send_response(200)
            self.end_headers()
            self.wfile.write(json.dumps({
                "nodeType": NODE_TYPE, "mips": MIPS,
                "cpuUsage": random.randint(5, 40),
            }).encode())
        else:
            self.send_response(404)
            self.end_headers()

    def log_message(self, format, *args):
        pass  # suppress logs

if __name__ == "__main__":
    port = int(os.environ.get("PORT", 5001))
    HTTPServer(("0.0.0.0", port), Handler).serve_forever()
