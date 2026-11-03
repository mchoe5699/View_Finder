// 뷰 파인더 (Bedrock) — 모서리·꼭짓점을 깎은 육면체로 지금 보는 방향을 화면에 보여 준다.
//
// HUD 표시: 리소스팩의 hud_screen.json 이 'vf:' 로 시작하는 제목(title)을 가로채서
// 미리 그려 둔 육면체 그림(textures/ui/view_finder/f<yaw>_<pitch>)을 화면 구석에 띄운다.
// 제목 형식:   vf:<위치 A~E><크기 1~3>|f<yaw 0~35>_<pitch 0~18>
// 부제목 형식: vfs:<방위 문구>
// 조작: 뷰 파인더 아이템 사용 → 26방향 선택 / 회전, 웅크리고 사용 → 설정.

import { system, world } from "@minecraft/server";
import { ActionFormData, ModalFormData } from "@minecraft/server-ui";

const ITEM_ID = "view_finder:view_finder";
const ICON_DIR = "textures/ui/view_finder/";
const REFRESH_TICKS = 100;

const POS_CODES = ["A", "B", "C", "D", "E"];
const POS_NAMES = ["오른쪽 위", "오른쪽 위 (모바일 버튼 아래)", "왼쪽 위", "오른쪽 아래", "왼쪽 아래"];
const SIZE_CODES = ["1", "2", "3"];
const SIZE_NAMES = ["작게", "보통", "크게"];

const DEFAULTS = { on: true, pos: 0, size: 1, heading: true, anim: true };

// ------------------------------------------------------------------ 설정 저장

function getSetting(player, key) {
  try {
    const v = player.getDynamicProperty("vf:" + key);
    return v === undefined ? DEFAULTS[key] : v;
  } catch {
    return DEFAULTS[key];
  }
}

function setSetting(player, key, value) {
  try {
    player.setDynamicProperty("vf:" + key, value);
  } catch {}
}

// ------------------------------------------------------------------ 방향 계산

const DIR8 = ["북", "북동", "동", "남동", "남", "남서", "서", "북서"];

function wrapDegrees(d) {
  d %= 360;
  if (d >= 180) d -= 360;
  if (d < -180) d += 360;
  return d;
}

function clamp(v, lo, hi) {
  return Math.max(lo, Math.min(hi, v));
}

function headingText(rot) {
  const bearing = (((rot.y + 180) % 360) + 360) % 360;
  const dir = DIR8[Math.round(bearing / 45) & 7];
  const deg = Math.round(bearing) % 360;
  const p = Math.round(rot.x);
  const pitch = p === 0 ? "수평" : p < 0 ? `위 ${-p}°` : `아래 ${p}°`;
  return `${dir} ${deg}° · ${pitch}`;
}

function frameOf(rot) {
  const yi = ((Math.round((wrapDegrees(rot.y) + 180) / 10) % 36) + 36) % 36;
  const pi = clamp(Math.round((rot.x + 90) / 10), 0, 18);
  return `f${yi}_${pi}`;
}

// ------------------------------------------------------------------ HUD 갱신

const lastSent = new Map(); // player.id -> { key, tick }

function hudTick() {
  const now = system.currentTick;
  for (const player of world.getAllPlayers()) {
    if (!player.isValid) continue;
    let title;
    let subtitle;
    const on = getSetting(player, "on");
    if (on) {
      const rot = player.getRotation();
      const pos = POS_CODES[clamp(getSetting(player, "pos"), 0, POS_CODES.length - 1)];
      const size = SIZE_CODES[clamp(getSetting(player, "size"), 0, SIZE_CODES.length - 1)];
      title = `vf:${pos}${size}|${frameOf(rot)}`;
      subtitle = "vfs:" + (getSetting(player, "heading") ? headingText(rot) : "");
    } else {
      title = "vf:OFF";
      subtitle = "vfs:";
    }
    const key = title + "\n" + subtitle;
    const last = lastSent.get(player.id);
    const changed = !last || last.key !== key;
    // 다른 제목에 덮여도 다시 나타나도록 켜져 있을 때는 주기적으로 다시 보낸다.
    if (changed || (on && now - last.tick >= REFRESH_TICKS)) {
      try {
        player.onScreenDisplay.setTitle(title, {
          subtitle,
          fadeInDuration: 0,
          stayDuration: on ? REFRESH_TICKS * 2 : 1,
          fadeOutDuration: 0,
        });
        lastSent.set(player.id, { key, tick: now });
      } catch {}
    }
  }
}

system.runInterval(hudTick, 1);

world.afterEvents.playerSpawn.subscribe((ev) => {
  const player = ev.player;
  lastSent.delete(player.id);
  if (!ev.initialSpawn) return;
  // 처음 들어온 플레이어에게 뷰 파인더 아이템을 한 번 준다.
  if (!player.getDynamicProperty("vf:given")) {
    system.runTimeout(() => {
      if (!player.isValid) return;
      try {
        player.runCommand(`give @s ${ITEM_ID} 1`);
        player.setDynamicProperty("vf:given", true);
        player.sendMessage("§b[뷰 파인더]§r 아이템을 사용하면 방향을 고를 수 있고, 웅크리고 사용하면 설정이 열립니다.");
      } catch {}
    }, 40);
  }
});

world.afterEvents.playerLeave.subscribe((ev) => {
  lastSent.delete(ev.playerId);
});

// ------------------------------------------------------------------ 시점 돌리기

const turning = new Map(); // player.id -> runId

function applyRotation(player, pitch, yaw) {
  const rotation = { x: clamp(pitch, -90, 90), y: yaw };
  try {
    player.teleport(player.location, { rotation, keepVelocity: true });
  } catch {
    try {
      player.teleport(player.location, { rotation });
    } catch {}
  }
}

function turnTo(player, yaw, pitch) {
  const prev = turning.get(player.id);
  if (prev !== undefined) system.clearRun(prev);
  const start = player.getRotation();
  const targetYaw = start.y + wrapDegrees(yaw - start.y);
  const steps = getSetting(player, "anim") ? 6 : 1;
  let i = 0;
  const id = system.runInterval(() => {
    if (!player.isValid) {
      system.clearRun(id);
      turning.delete(player.id);
      return;
    }
    i++;
    const t = i / steps;
    const e = 1 - Math.pow(1 - t, 3);
    applyRotation(player, start.x + (pitch - start.x) * e, start.y + (targetYaw - start.y) * e);
    if (i >= steps) {
      system.clearRun(id);
      turning.delete(player.id);
    }
  }, 1);
  turning.set(player.id, id);
}

function lookAngles(d) {
  const len = Math.hypot(d[0], d[1], d[2]);
  const nx = d[0] / len;
  const ny = d[1] / len;
  const nz = d[2] / len;
  if (d[0] === 0 && d[2] === 0) return null; // 바로 위/아래: 방위는 그대로
  return {
    yaw: (Math.atan2(-nx, nz) * 180) / Math.PI,
    pitch: (-Math.asin(ny) * 180) / Math.PI,
  };
}

// ------------------------------------------------------------------ 방향 목록

function hName(dx, dz) {
  if (dz < 0) return dx > 0 ? "북동" : dx < 0 ? "북서" : "북";
  if (dz > 0) return dx > 0 ? "남동" : dx < 0 ? "남서" : "남";
  if (dx > 0) return "동";
  if (dx < 0) return "서";
  return null;
}

function dirName(d) {
  const h = hName(d[0], d[2]);
  const v = d[1] > 0 ? "위" : d[1] < 0 ? "아래" : null;
  if (!h) return v;
  return v ? `${h} · ${v}` : h;
}

function iconOf(d) {
  return ICON_DIR + "icon_" + d.map((c) => (c > 0 ? "p" : c < 0 ? "n" : "z")).join("_");
}

const DIRECTIONS = [
  // 정면 6
  ["면", [0, 0, -1]], ["면", [1, 0, 0]], ["면", [0, 0, 1]], ["면", [-1, 0, 0]], ["면", [0, 1, 0]], ["면", [0, -1, 0]],
  // 모서리 12
  ["모서리", [1, 0, -1]], ["모서리", [1, 0, 1]], ["모서리", [-1, 0, 1]], ["모서리", [-1, 0, -1]],
  ["모서리", [0, 1, -1]], ["모서리", [1, 1, 0]], ["모서리", [0, 1, 1]], ["모서리", [-1, 1, 0]],
  ["모서리", [0, -1, -1]], ["모서리", [1, -1, 0]], ["모서리", [0, -1, 1]], ["모서리", [-1, -1, 0]],
  // 꼭짓점 8
  ["꼭짓점", [1, 1, -1]], ["꼭짓점", [1, 1, 1]], ["꼭짓점", [-1, 1, 1]], ["꼭짓점", [-1, 1, -1]],
  ["꼭짓점", [1, -1, -1]], ["꼭짓점", [1, -1, 1]], ["꼭짓점", [-1, -1, 1]], ["꼭짓점", [-1, -1, -1]],
];

// ------------------------------------------------------------------ 폼

function showMain(player) {
  const rot = player.getRotation();
  const form = new ActionFormData().title("뷰 파인더").body(`지금 보는 방향: ${headingText(rot)}\n누른 방향이 정면이 되도록 돌아봅니다.`);
  const actions = [];

  for (const [kind, d] of DIRECTIONS) {
    form.button(`${dirName(d)}\n§8${kind}`, iconOf(d));
    actions.push(() => {
      const a = lookAngles(d);
      if (a) turnTo(player, a.yaw, a.pitch);
      else turnTo(player, player.getRotation().y, d[1] > 0 ? -90 : 90);
    });
  }

  const turns = [
    ["왼쪽으로 45°", -45, 0],
    ["오른쪽으로 45°", 45, 0],
    ["위로 45°", 0, -45],
    ["아래로 45°", 0, 45],
    ["뒤로 돌기", 180, 0],
  ];
  for (const [label, dy, dp] of turns) {
    form.button(`${label}\n§8회전`);
    actions.push(() => {
      const r = player.getRotation();
      turnTo(player, r.y + dy, clamp(r.x + dp, -90, 90));
    });
  }
  form.button("수평으로\n§8회전");
  actions.push(() => turnTo(player, player.getRotation().y, 0));

  form.button("설정\n§8위치·크기·표시");
  actions.push(() => showSettings(player));

  form
    .show(player)
    .then((res) => {
      if (res.canceled || res.selection === undefined) return;
      const act = actions[res.selection];
      if (act) act();
    })
    .catch(() => {});
}

function showSettings(player) {
  const form = new ModalFormData()
    .title("뷰 파인더 설정")
    .toggle("화면에 표시", { defaultValue: !!getSetting(player, "on") })
    .dropdown("위치", POS_NAMES, { defaultValueIndex: clamp(getSetting(player, "pos"), 0, POS_NAMES.length - 1) })
    .dropdown("크기", SIZE_NAMES, { defaultValueIndex: clamp(getSetting(player, "size"), 0, SIZE_NAMES.length - 1) })
    .toggle("방위·각도 글자 표시", { defaultValue: !!getSetting(player, "heading") })
    .toggle("부드럽게 돌아보기", { defaultValue: !!getSetting(player, "anim") });

  form
    .show(player)
    .then((res) => {
      if (res.canceled || !res.formValues) return;
      const [on, pos, size, heading, anim] = res.formValues;
      setSetting(player, "on", !!on);
      setSetting(player, "pos", Number(pos) || 0);
      setSetting(player, "size", Number(size) || 0);
      setSetting(player, "heading", !!heading);
      setSetting(player, "anim", !!anim);
      lastSent.delete(player.id);
    })
    .catch(() => {});
}

world.afterEvents.itemUse.subscribe((ev) => {
  if (ev.itemStack?.typeId !== ITEM_ID) return;
  const player = ev.source;
  if (player.isSneaking) showSettings(player);
  else showMain(player);
});
