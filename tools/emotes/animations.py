"""Writes the bundled emote animations as Bedrock animation JSON (Blockbench / GeckoLib format).

Run from the repository root: python3 tools/emotes/animations.py
Values use the Java player rig: rotations in degrees (negative x swings an arm forward, positive z
lifts the right arm sideways, negative z the left arm), positions in pixels with +y up.
Keeping the source here makes the poses easy to tweak; the JSON files are what the client loads.
"""
import json
import math
import os

OUT = "src/main/resources/assets/archangelclient/emotes/animations"
REST = [0, 0, 0]


def key(v, easing=None):
    return {"vector": v, "easing": easing} if easing else v


def track(*frames):
    """frames: (time, vector[, easing]) -> Bedrock keyframe map."""
    out = {}
    for f in frames:
        out[f"{f[0]:.2f}".rstrip("0").rstrip(".") if f[0] else "0.0"] = key(f[1], f[2] if len(f) > 2 else None)
    return out


def animation(name, length, loop, bones):
    return {"format_version": "1.8.0", "animations": {f"animation.archangel.{name}": {
        "loop": loop, "animation_length": length, "bones": bones}}}


def wave():
    up = lambda z: [-12, 0, z]
    return animation("wave", 2.4, False, {
        "right_arm": {"rotation": track(
            (0, REST), (0.35, up(150), "easeOutBack"), (0.6, up(122), "easeInOutSine"), (0.85, up(158), "easeInOutSine"),
            (1.1, up(122), "easeInOutSine"), (1.35, up(158), "easeInOutSine"), (1.6, up(125), "easeInOutSine"),
            (1.9, up(145), "easeInOutSine"), (2.4, REST, "easeInOutSine"))},
        "head": {"rotation": track((0, REST), (0.4, [0, 0, -6], "easeOutSine"), (1.9, [0, 0, -6]), (2.4, REST, "easeInOutSine"))},
        "body": {"rotation": track((0, REST), (0.4, [0, 0, -2], "easeOutSine"), (1.9, [0, 0, -2]), (2.4, REST, "easeInOutSine"))},
    })


def dab():
    right = [-25, 0, 128]
    left = [-112, 52, 0]
    return animation("dab", 2.0, False, {
        "right_arm": {"rotation": track((0, REST), (0.25, right, "easeOutBack"), (1.45, right), (2.0, REST, "easeInOutSine"))},
        "left_arm": {"rotation": track((0, REST), (0.25, left, "easeOutBack"), (1.45, left), (2.0, REST, "easeInOutSine"))},
        "head": {"rotation": track((0, REST), (0.25, [38, 28, 0], "easeOutBack"), (1.45, [38, 28, 0]), (2.0, REST, "easeInOutSine"))},
        "body": {"rotation": track((0, REST), (0.25, [6, 8, 0], "easeOutBack"), (1.45, [6, 8, 0]), (2.0, REST, "easeInOutSine"))},
    })


def clap():
    def arm(side, closed):
        return [-72, side * (26 if closed else 6), 0]
    period = 0.5
    return animation("clap", period, True, {
        "right_arm": {"rotation": track((0, arm(-1, False)), (0.17, arm(-1, True), "easeInQuad"), (period, arm(-1, False), "easeOutQuad"))},
        "left_arm": {"rotation": track((0, arm(1, False)), (0.17, arm(1, True), "easeInQuad"), (period, arm(1, False), "easeOutQuad"))},
        "head": {"rotation": track((0, [4, 0, 0]), (0.17, [-2, 0, 0], "easeInQuad"), (period, [4, 0, 0], "easeOutQuad"))},
    })


def floss():
    # A: arms swing to the player's right (left arm in front), hips to the left; B mirrors it
    a = {"right_arm": [22, 0, 38], "left_arm": [-22, 0, 34], "root": [1.4, 0, 0], "body": [0, 0, -5]}
    b = {"right_arm": [-22, 0, -34], "left_arm": [22, 0, -38], "root": [-1.4, 0, 0], "body": [0, 0, 5]}
    e = "easeInOutSine"
    bones = {}
    for bone in ("right_arm", "left_arm", "body"):
        bones[bone] = {"rotation": track((0, a[bone]), (0.4, b[bone], e), (0.8, a[bone], e))}
    bones["root"] = {"position": track((0, a["root"]), (0.4, b["root"], e), (0.8, a["root"], e))}
    bones["right_leg"] = {"rotation": track((0, [0, 0, 4]), (0.4, [0, 0, -4], e), (0.8, [0, 0, 4], e))}
    bones["left_leg"] = {"rotation": track((0, [0, 0, 4]), (0.4, [0, 0, -4], e), (0.8, [0, 0, 4], e))}
    bones["head"] = {"rotation": track((0, [0, 0, 4]), (0.4, [0, 0, -4], e), (0.8, [0, 0, 4], e))}
    return animation("floss", 0.8, True, bones)


def sit():
    e = "easeOutCubic"
    return animation("sit", 0.6, "hold_on_last_frame", {
        "root": {"position": track((0, REST), (0.6, [0, -10, 0], e))},
        "right_leg": {"rotation": track((0, REST), (0.6, [-86, 14, 0], e))},
        "left_leg": {"rotation": track((0, REST), (0.6, [-86, -14, 0], e))},
        "right_arm": {"rotation": track((0, REST), (0.6, [-38, 0, 8], e))},
        "left_arm": {"rotation": track((0, REST), (0.6, [-38, 0, -8], e))},
        "body": {"rotation": track((0, REST), (0.6, [-4, 0, 0], e))},
        "head": {"rotation": track((0, REST), (0.6, [-4, 0, 0], e))},
    })


def bow():
    # bend from the hips: the body turns around its top (the neck), so the neck, head and shoulders
    # are moved to keep the hips where they are
    deg = 50
    c, s = math.cos(math.radians(deg)), math.sin(math.radians(deg))
    neck = [0, -round(12 * (1 - c), 2), -round(12 * s, 2)]
    shoulder = [0, -round(10 * (1 - c), 2), -round(10 * s, 2)]
    e = "easeInOutSine"

    def bent(v):
        return track((0, REST), (0.55, v, e), (1.35, v), (2.0, REST, e))

    return animation("bow", 2.2, False, {
        "body": {"rotation": bent([deg, 0, 0]), "position": bent(neck)},
        "head": {"rotation": bent([deg + 8, 0, 0]), "position": bent(neck)},
        "right_arm": {"rotation": bent([deg - 85, -38, 0]), "position": bent(shoulder)},
        "left_arm": {"rotation": bent([deg + 32, 0, -6]), "position": bent(shoulder)},
    })


def salute():
    arm = [-142, -48, 0]
    return animation("salute", 2.0, False, {
        "right_arm": {"rotation": track((0, REST), (0.3, arm, "easeOutBack"), (1.5, arm), (2.0, REST, "easeInOutSine"))},
        "head": {"rotation": track((0, REST), (0.3, [-6, 0, 0], "easeOutSine"), (1.5, [-6, 0, 0]), (2.0, REST, "easeInOutSine"))},
    })


def cheer():
    e = "easeInOutSine"
    return animation("cheer", 0.7, True, {
        "right_arm": {"rotation": track((0, [-10, 0, 165]), (0.35, [-10, 0, 140], e), (0.7, [-10, 0, 165], e))},
        "left_arm": {"rotation": track((0, [-10, 0, -165]), (0.35, [-10, 0, -140], e), (0.7, [-10, 0, -165], e))},
        "root": {"position": track((0, [0, 1.5, 0]), (0.35, REST, "easeInQuad"), (0.7, [0, 1.5, 0], "easeOutQuad"))},
        "head": {"rotation": track((0, [-12, 0, 0]), (0.35, [-6, 0, 0], e), (0.7, [-12, 0, 0], e))},
    })


EMOTES = {"wave": wave, "dab": dab, "clap": clap, "floss": floss, "sit": sit, "bow": bow, "salute": salute, "cheer": cheer}

if __name__ == "__main__":
    os.makedirs(OUT, exist_ok=True)
    for name, build in EMOTES.items():
        with open(os.path.join(OUT, f"{name}.animation.json"), "w") as f:
            json.dump(build(), f, indent=2)
            f.write("\n")
    print(f"wrote {len(EMOTES)} animations to {OUT}")
