"""Local card catalog editor for the Android app.

Serves a small web page that edits app/src/main/assets/cards.json and saves card
pictures into app/src/main/assets/cards/. Only listens on localhost.

    python3 tools/card-editor/server.py [port]

Then open http://localhost:8000 in a browser.
"""

import json
import re
import sys
from http import HTTPStatus
from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
from pathlib import Path
from urllib.parse import unquote, urlparse

HERE = Path(__file__).resolve().parent
ASSETS = HERE.parent.parent / "app" / "src" / "main" / "assets"
CATALOG = ASSETS / "cards.json"
IMAGES = ASSETS / "cards"

CATEGORIES = {"BASIC_ACTION", "ADVANCED_ACTION", "SPELL", "UNIT", "ARTIFACT", "TACTIC"}
COLORED_CATEGORIES = {"BASIC_ACTION", "ADVANCED_ACTION", "SPELL"}
COLORS = {"RED", "BLUE", "WHITE", "GREEN"}
# Base-game Heroes; must match HEROES in the app's game/Rules.kt
HEROES = {"Arythea", "Goldyx", "Norowas", "Tovak"}
ID_PATTERN = re.compile(r"^[a-z0-9_]+$")
IMAGE_PATTERN = re.compile(r"^[a-z0-9_]+\.(jpg|png|webp)$")
IMAGE_TYPES = {"image/jpeg": "jpg", "image/png": "png", "image/webp": "webp"}
MAX_IMAGE_BYTES = 5 * 1024 * 1024


def validate(cards):
    """Returns a list of problems with the catalog, empty when it can be saved."""
    if not isinstance(cards, list):
        return ["The catalog must be a list of cards"]
    problems = []
    seen = set()
    for i, card in enumerate(cards):
        where = f"Card {i + 1} ({card.get('name') or 'no name'})" if isinstance(card, dict) else f"Card {i + 1}"
        if not isinstance(card, dict):
            problems.append(f"{where}: not an object")
            continue
        card_id = card.get("id", "")
        if not ID_PATTERN.match(card_id):
            problems.append(f"{where}: id must use only a-z, 0-9 and _")
        elif card_id in seen:
            problems.append(f"{where}: duplicate id '{card_id}'")
        seen.add(card_id)
        if not str(card.get("name", "")).strip():
            problems.append(f"{where}: missing name")
        if card.get("category") not in CATEGORIES:
            problems.append(f"{where}: unknown category")
        if card.get("category") in COLORED_CATEGORIES and card.get("color") not in COLORS:
            problems.append(f"{where}: needs a color")
        if card.get("category") == "BASIC_ACTION" and card.get("hero") not in HEROES:
            problems.append(f"{where}: Basic Actions need the Hero whose deck they belong to")
        if card.get("category") == "TACTIC":
            if card.get("number") not in range(1, 7):
                problems.append(f"{where}: Tactics need their number, 1 to 6")
            if not isinstance(card.get("night"), bool):
                problems.append(f"{where}: Tactics must be Day or Night")
        image = card.get("image")
        if image is not None and not (IMAGE_PATTERN.match(image) and (IMAGES / image).is_file()):
            problems.append(f"{where}: image '{image}' not found")
    return problems


class Handler(BaseHTTPRequestHandler):
    def do_GET(self):
        path = urlparse(self.path).path
        if path in ("/", "/index.html"):
            self.send_file(HERE / "index.html", "text/html; charset=utf-8")
        elif path == "/api/cards":
            self.send_file(CATALOG, "application/json") if CATALOG.exists() else self.send_json([])
        elif path.startswith("/images/"):
            name = unquote(path[len("/images/"):])
            if not IMAGE_PATTERN.match(name):
                return self.send_error(HTTPStatus.NOT_FOUND)
            ext = name.rsplit(".", 1)[1]
            self.send_file(IMAGES / name, "image/jpeg" if ext == "jpg" else f"image/{ext}")
        else:
            self.send_error(HTTPStatus.NOT_FOUND)

    def do_PUT(self):
        if urlparse(self.path).path != "/api/cards":
            return self.send_error(HTTPStatus.NOT_FOUND)
        try:
            cards = json.loads(self.read_body())
        except ValueError:
            return self.send_json({"errors": ["Invalid JSON"]}, HTTPStatus.BAD_REQUEST)
        problems = validate(cards)
        if problems:
            return self.send_json({"errors": problems}, HTTPStatus.BAD_REQUEST)
        CATALOG.write_text(json.dumps(cards, indent=2, ensure_ascii=False) + "\n", encoding="utf-8")
        self.send_json({"saved": len(cards)})

    def do_POST(self):
        # POST /api/images/<card id> with the image bytes as the body
        path = urlparse(self.path).path
        card_id = path[len("/api/images/"):] if path.startswith("/api/images/") else ""
        ext = IMAGE_TYPES.get(self.headers.get("Content-Type", ""))
        if not ID_PATTERN.match(card_id) or ext is None:
            return self.send_json({"errors": ["Bad card id or image type"]}, HTTPStatus.BAD_REQUEST)
        body = self.read_body()
        if not body or len(body) > MAX_IMAGE_BYTES:
            return self.send_json({"errors": ["Image is empty or too large"]}, HTTPStatus.BAD_REQUEST)
        IMAGES.mkdir(parents=True, exist_ok=True)
        name = f"{card_id}.{ext}"
        # Drop the card's older picture in another format, if any
        for old in IMAGES.glob(f"{card_id}.*"):
            if old.name != name:
                old.unlink()
        (IMAGES / name).write_bytes(body)
        self.send_json({"image": name})

    def do_DELETE(self):
        path = urlparse(self.path).path
        name = unquote(path[len("/api/images/"):]) if path.startswith("/api/images/") else ""
        if not IMAGE_PATTERN.match(name):
            return self.send_error(HTTPStatus.NOT_FOUND)
        (IMAGES / name).unlink(missing_ok=True)
        self.send_json({"deleted": name})

    def read_body(self):
        length = int(self.headers.get("Content-Length", 0))
        if length > MAX_IMAGE_BYTES:
            return b""
        return self.rfile.read(length)

    def send_file(self, path, content_type):
        if not path.is_file():
            return self.send_error(HTTPStatus.NOT_FOUND)
        data = path.read_bytes()
        self.send_response(HTTPStatus.OK)
        self.send_header("Content-Type", content_type)
        self.send_header("Content-Length", str(len(data)))
        self.send_header("Cache-Control", "no-store")
        self.end_headers()
        self.wfile.write(data)

    def send_json(self, value, status=HTTPStatus.OK):
        data = json.dumps(value).encode()
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(data)))
        self.end_headers()
        self.wfile.write(data)


if __name__ == "__main__":
    port = int(sys.argv[1]) if len(sys.argv) > 1 else 8000
    server = ThreadingHTTPServer(("127.0.0.1", port), Handler)
    print(f"Card editor: http://localhost:{port}  (editing {CATALOG})")
    try:
        server.serve_forever()
    except KeyboardInterrupt:
        pass
