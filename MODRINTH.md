# Better Arrows — для выкладки на Modrinth

---

# ЧАСТЬ 1. Поля формы (не часть описания)

| Поле | Значение |
|---|---|
| **Name** | Better Arrows |
| **Slug** | `better-arrows` |
| **Summary** | Arrows stick exactly where they hit and stay on the limb they hit — and blocks answer back: ricochets off stone, arrows sliding on ice. |
| **Type / Loader** | Mod / Fabric |
| **Game versions** | 26.2 |
| **Client side** | **Required** |
| **Server side** | **Required** |
| **License** | MIT |
| **Categories** | Game Mechanics, Mobs |

**Зависимости:** Fabric API — Required · Cloth Config — Required · Mod Menu — Optional

**Ссылки в форме:** Source code — `https://github.com/Mixaold/BetterArrows`, Issue tracker — тот же адрес с `/issues`. Wiki и Discord — пусто. Donation — DonationAlerts через «Other».

**Client и Server оба Required** потому, что сервер ловит точку попадания и решает рикошет, а клиент рисует. Тут, в отличие от Better Mob Particles, никого не выкинет: при разных версиях мод просто молча ничего не делает — id мода входит в имя сетевого канала.

**Скриншоты в галерею:** стрела в голове зомби, стрелы в двух разных по форме мобах, рикошет от камня, стрела, едущая по льду, экран настроек.

**Иконка проекта:** `icons/Arrow MOD new 512.png`.

---
---

# ЧАСТЬ 2. Описание (English)

## Better Arrows

Arrows stay where they hit, stuck in whatever body part they went into — they swing with the arm,
bend with the leg. Vanilla just counts how many arrows are in you and re-rolls a random spot on a
random body part every single frame: you nail a zombie in the head, and the arrow turns up in its
leg. And on mobs vanilla doesn't draw them at all.

Blocks push back too. Shoot flat and it skips off stone; off wood you have to come in twice as flat.
Sand swallows the arrow unless it hits almost sideways. Wool, mud and snow never bounce it, ever.
The steeper the hit, the more speed stays in the block. On ice the arrow slides along the surface
instead of getting kicked off it like glass. And every material has its own hit sound and its own
spray of debris.

### What a flat shot does

| Surface | What happens |
|---|---|
| Stone, metal, glass, deepslate | Skips off, even from a fairly steep angle |
| Wood, bamboo, ladders | Skips off, but you have to shoot twice as flat |
| Sand, gravel, dirt | Only a sliver near flat — anything steeper is swallowed |
| Wool, mud, snow, slime, cobweb | Never bounces. The arrow sticks |
| Ice | Slides *along* the surface, slowing as it goes |
| Leaves | Vanilla: the arrow just stands in the canopy |

### Where the arrow ends up

* **The exact point** — the mod grabs the 3D hit point vanilla computes on impact and then throws away.
* **The right limb** — the arrow belongs to the body part it hit and moves with it.
* **The real angle** — taken from the arrow's flight direction, not guessed.
* **Everyone** — players and mobs, vanilla and modded. No arrows on endermen, same as vanilla.
* **Still there after a rejoin**, and a player who joins later sees them too.
* They work their way out over time, the way vanilla's arrow count decays.

### Settings

**Mod Menu → Better Arrows.** Every part above can be switched off on its own. On a server the
server's file decides the gameplay half — ricochet, ice, sounds, debris — and each client's file
decides the drawing half.

> ### ⚠️ Install it on the client AND the server
> Same version on both sides. Nothing crashes if they differ — the mod just quietly does nothing,
> which is harder to notice. If a friend sees no arrows, they've got a different version.

Resource packs that replace entity models (Fresh Animations through Entity Model Features) work, but
on a heavily reshaped model the odd arrow will be missing rather than misplaced — the mod would
rather draw nothing than guess.

### Requires

Fabric · Minecraft 26.2 · Fabric API · Cloth Config · Mod Menu (optional, for the settings button)

MIT license — use it, fork it, put it in your modpack. Mobs getting visible stuck arrows at all was
first done by [Arrow In The Knee](https://modrinth.com/mod/aitk), with vanilla's random placement;
this one tracks the real hit point.

---
---

# ЧАСТЬ 3. Описание (Русский)

## Better Arrows

Стрела торчит там, куда воткнулась, и держится за ту часть тела, в которую вошла: рука дёрнулась —
стрела с ней, нога согнулась — и стрела гнётся. А ваниль помнит только, сколько стрел в цели, и
каждый кадр заново кидает кубик: случайное место на случайной части тела. Всадил зомби в голову —
глядь, стрела в ноге. А на мобах ваниль их и вовсе не рисует.

Блоки тоже не молчат. Стрельни по камню вскользь, почти вдоль стены — стрела чиркнёт и улетит
дальше. С деревом так же, только стрелять надо ещё ровнее. Песок стрелу проглотит, если она пришла не
совсем уж плашмя. Шерсть, грязь и снег не отбрасывают её вообще никогда. Чем прямее попал, тем
сильнее стрела вязнет в блоке. По льду она едет вдоль поверхности, а не отскакивает, как от стекла.
И у каждого материала свой звук удара и своя крошка.

### Что будет, если стрела чиркнёт по блоку

| По чему стреляешь | Что будет |
|---|---|
| Камень, металл, стекло, глубинный сланец | Отскочит, даже если попал довольно прямо |
| Дерево, бамбук, лестницы | Тоже отскочит, но стрелять надо ровнее, почти вдоль |
| Песок, гравий, земля | Отскочит, только если прошла впритирку. Чуть прямее — проглотит |
| Шерсть, грязь, снег, слизь, паутина | Не отскакивает никогда, стрела просто втыкается |
| Лёд | Не отскакивает, а едет по льду и постепенно тормозит |
| Листва | Как в обычной игре: стрела торчит в листьях |

### Куда воткнётся стрела

* **Ровно туда, куда попал.** Игра сама считает место удара, а потом выбрасывает его — мод его забирает себе.
* **В ту самую руку или ногу**, в которую вошла. Рука двигается — стрела с ней.
* **Под тем углом, под которым летела**, а не под каким попало.
* **В кого угодно** — в игроков и в мобов, хоть из ванили, хоть из другого мода. В эндермене стрел не будет, как и в обычной игре.
* **Перезашёл — стрелы на месте.** И друг, который зашёл позже, их тоже увидит.
* Со временем сами вылезают и пропадают, как и в обычной игре.

### Настройки

**Mod Menu → Better Arrows.** Что угодно можно выключить по отдельности. На сервере за отскоки, лёд,
звуки и крошку отвечают настройки сервера, а за то, как стрелы нарисованы, — настройки каждого
игрока.

> ### ⚠️ Ставить и на клиент, и на сервер
> Версия одна и та же с обеих сторон. Ничего не упадёт, если они разные, — мод просто молча ничего
> не сделает, что заметить сложнее. Если друг не видит стрел, у него другая версия.

Ресурспаки, которые меняют модели мобов (Fresh Animations через Entity Model Features), работают. Но
если модель перекроена сильно, какая-нибудь стрела может вообще не появиться: мод лучше не нарисует
ничего, чем воткнёт её мимо.

### Требуется

Fabric · Minecraft 26.2 · Fabric API · Cloth Config · Mod Menu (по желанию, ради кнопки настроек)

Лицензия MIT — пользуйтесь, форкайте, кладите в сборки. Стрелы на мобах вообще первым показал
[Arrow In The Knee](https://modrinth.com/mod/aitk) — правда, куда попало; этот мод втыкает их туда,
куда ты попал.
