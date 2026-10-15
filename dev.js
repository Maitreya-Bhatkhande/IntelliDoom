// dev.js
// ---------------------------------------------------------------------------
// One command to start EVERYTHING:
//
//     npm run dev        (from the project root)
//
// It starts the Kotlin backend (backend\run.bat) and the Vite frontend side
// by side, and prints both of their logs in this one terminal.
// Press Ctrl+C to stop both.
//
// It uses only Node's built-in "child_process", so there is nothing to install.
// ---------------------------------------------------------------------------

import { spawn } from "node:child_process";

// Start one server and forward its output to this terminal.
function startBackend() {
  const child = spawn("cmd.exe", ["/c", "run.bat"], {
    cwd: "backend",
    stdio: "inherit",
  });

  child.on("exit", (code) => {
    console.log(`[backend] stopped (exit code ${code}).`);
  });

  return child;
}

function startFrontend() {
  const child = spawn("npm", ["run", "dev"], {
    cwd: "frontend",
    shell: true,
    stdio: "inherit",
  });

  child.on("exit", (code) => {
    console.log(`[frontend] stopped (exit code ${code}).`);
  });

  return child;
}

const backend = startBackend();
const frontend = startFrontend();

// When this script is stopped (Ctrl+C), stop the two servers as well.
process.on("SIGINT", () => {
  backend.kill();
  frontend.kill();
  process.exit(0);
});

console.log("Starting backend (http://localhost:4000) and frontend (http://localhost:5173)...");
