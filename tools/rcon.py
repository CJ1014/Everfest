#!/usr/bin/env python3
"""Minimal RCON client for driving a dev server in tests: rcon.py [--port N] [--password P] cmd [cmd ...]"""
import argparse
import socket
import struct
import sys
import time


def packet(req_id, ptype, body):
    data = struct.pack("<ii", req_id, ptype) + body.encode("utf-8") + b"\x00\x00"
    return struct.pack("<i", len(data)) + data


def recv_packet(sock):
    raw = b""
    while len(raw) < 4:
        chunk = sock.recv(4 - len(raw))
        if not chunk:
            raise ConnectionError("closed")
        raw += chunk
    (length,) = struct.unpack("<i", raw)
    data = b""
    while len(data) < length:
        chunk = sock.recv(length - len(data))
        if not chunk:
            raise ConnectionError("closed")
        data += chunk
    req_id, ptype = struct.unpack("<ii", data[:8])
    return req_id, ptype, data[8:-2].decode("utf-8", "replace")


def main():
    ap = argparse.ArgumentParser()
    ap.add_argument("--host", default="127.0.0.1")
    ap.add_argument("--port", type=int, default=25575)
    ap.add_argument("--password", default="everfest")
    ap.add_argument("--wait", type=float, default=0, help="retry connecting for this many seconds")
    ap.add_argument("commands", nargs="+")
    a = ap.parse_args()
    deadline = time.time() + a.wait
    while True:
        try:
            sock = socket.create_connection((a.host, a.port), timeout=10)
            break
        except OSError:
            if time.time() > deadline:
                raise
            time.sleep(1)
    sock.sendall(packet(1, 3, a.password))
    rid, _, _ = recv_packet(sock)
    if rid == -1:
        print("auth failed", file=sys.stderr)
        sys.exit(1)
    for i, cmd in enumerate(a.commands, start=2):
        sock.sendall(packet(i, 2, cmd))
        _, _, body = recv_packet(sock)
        print(f"> {cmd}\n{body}")
    sock.close()


if __name__ == "__main__":
    main()
