"""리소스팩 ui/hud_screen.json 생성: 위치 5종 x 크기 3종 뷰 파인더 HUD."""
import json
import os
import sys

OUT = sys.argv[1] if len(sys.argv) > 1 else "resource_pack"

POSITIONS = {
    # code: (anchor, offset, label below?)
    "A": ("top_right", [-4, 4], True),      # 오른쪽 위
    "B": ("top_right", [-4, 30], True),     # 오른쪽 위 (모바일: 버튼 아래)
    "C": ("top_left", [4, 4], True),        # 왼쪽 위
    "D": ("bottom_right", [-4, -4], False), # 오른쪽 아래
    "E": ("bottom_left", [4, -4], False),   # 왼쪽 아래
}
SIZES = {"1": 48, "2": 64, "3": 96}
TITLE = "#hud_title_text_string"
SUB = "#hud_subtitle_text_string"


def contains(binding, s):
    return f"(not (({binding} - '{s}') = {binding}))"


def variant(code, size_code):
    anchor, offset, below = POSITIONS[code]
    px = SIZES[size_code]
    tag = f"vf:{code}{size_code}|"
    label_h = 11
    cube = {
        "type": "image",
        "size": [px, px],
        "anchor_from": "top_middle" if below else "bottom_middle",
        "anchor_to": "top_middle" if below else "bottom_middle",
        "texture": "#texture",
        "alpha": 0.95,
        "bindings": [
            {"binding_name": TITLE, "binding_type": "global"},
            {
                "binding_type": "view",
                "source_property_name": f"('textures/ui/view_finder/' + ({TITLE} - '{tag}'))",
                "target_property_name": "#texture",
            },
        ],
    }
    heading = {
        "type": "label",
        "size": ["default", 10],
        "anchor_from": "bottom_middle" if below else "top_middle",
        "anchor_to": "bottom_middle" if below else "top_middle",
        "text": "#text",
        "color": [1, 1, 1],
        "shadow": True,
        "localize": False,
        "font_scale_factor": 1.0 if px >= 64 else 0.8,
        "text_alignment": "center",
        "bindings": [
            {"binding_name": SUB, "binding_type": "global"},
            {"binding_type": "view", "source_property_name": f"({SUB} - 'vfs:')", "target_property_name": "#text"},
            {"binding_type": "view", "source_property_name": contains(SUB, "vfs:"), "target_property_name": "#visible"},
        ],
    }
    return {
        "type": "panel",
        "size": [px, px + label_h],
        "anchor_from": anchor,
        "anchor_to": anchor,
        "offset": offset,
        "layer": 20,
        "bindings": [
            {"binding_name": TITLE, "binding_type": "global"},
            {"binding_type": "view", "source_property_name": contains(TITLE, tag), "target_property_name": "#visible"},
        ],
        "controls": [{"cube": cube}, {"heading": heading}],
    }


def main():
    ui = {
        "namespace": "hud",
        # 뷰 파인더가 보내는 'vf:' 제목은 화면 가운데에 띄우지 않는다.
        "hud_title_text": {
            "bindings": [
                {"binding_name": TITLE, "binding_type": "global"},
                {
                    "binding_type": "view",
                    "source_property_name": f"(({TITLE} - 'vf:') = {TITLE})",
                    "target_property_name": "#visible",
                },
            ]
        },
        "root_panel": {
            "modifications": [
                {
                    "array_name": "controls",
                    "operation": "insert_back",
                    "value": {"view_finder_root@hud.view_finder_root": {}},
                }
            ]
        },
        "view_finder_root": {
            "type": "panel",
            "size": ["100%", "100%"],
            "layer": 20,
            "controls": [],
        },
    }
    for code in POSITIONS:
        for sc in SIZES:
            name = f"view_finder_{code}{sc}"
            ui[name] = variant(code, sc)
            ui["view_finder_root"]["controls"].append({f"{name}@hud.{name}": {}})
    os.makedirs(os.path.join(OUT, "ui"), exist_ok=True)
    with open(os.path.join(OUT, "ui", "hud_screen.json"), "w", encoding="utf-8") as f:
        json.dump(ui, f, ensure_ascii=False, indent=2)
    print("variants:", len(POSITIONS) * len(SIZES))


if __name__ == "__main__":
    main()
