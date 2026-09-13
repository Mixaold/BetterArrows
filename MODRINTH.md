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

Блоки тоже не молчат. Пустил полого — от камня отскочит, от дерева отскочит, только если вдвое
положе, а песок стрелу проглотит, если она пришла не почти плашмя. Шерсть, грязь и снег не
отбрасывают её вообще никогда. Чем круче удар, тем больше скорости остаётся в блоке. По льду стрела
едет вдоль поверхности, а не отпинывается, как от стекла. И у каждого материала свой звук попадания
и своя крошка.

### Что делает пологий выстрел

| Поверхность | Что происходит |
|---|---|
| Камень, металл, стекло, глубинный сланец | Отскакивает даже с довольно крутого угла |
| Дерево, бамбук, лестницы | Отскакивает, но целиться надо вдвое положе |
| Песок, гравий, земля | Только у самой плоскости — всё, что круче, проглатывается |
| Шерсть, грязь, снег, слизь, паутина | Не отскакивает никогда, стрела втыкается |
| Лёд | Едет *вдоль* поверхности, постепенно замедляясь |
| Листва | По-ванильному: стрела просто торчит в кроне |

### Куда попадает стрела

* **Точная точка** — мод перехватывает 3D-координату, которую ваниль считает при попадании и тут же забывает.
* **Нужная конечность** — стрела принадлежит той части тела, в которую вошла, и двигается с ней.
* **Настоящий угол** — из реального направления полёта, а не угаданный.
* **Все подряд** — игроки и мобы, ванильные и модовые. На эндермене стрел нет, как и в ванили.
* **На месте после перезахода**, и зашедший позже игрок их тоже увидит.
* Со временем выходят сами, по мере убывания ванильного счётчика стрел.

### Настройки

**Mod Menu → Better Arrows.** Любую часть можно выключить отдельно. На сервере серверный файл решает
геймплейную половину — рикошет, лёд, звуки, крошку, — а файл каждого клиента решает отрисовку.

> ### ⚠️ Ставить и на клиент, и на сервер
> Версия одна и та же с обеих сторон. Ничего не упадёт, если они разные, — мод просто молча ничего
> не сделает, что заметить сложнее. Если друг не видит стрел, у него другая версия.

Ресурспаки, заменяющие модели сущностей (Fresh Animations через Entity Model Features), работают, но
на сильно перекроенной модели отдельная стрела скорее не появится, чем встанет не туда: мод
предпочитает не рисовать ничего, вместо того чтобы гадать.

### Требуется

Fabric · Minecraft 26.2 · Fabric API · Cloth Config · Mod Menu (по желанию, ради кнопки настроек)

Лицензия MIT — пользуйтесь, форкайте, кладите в сборки. Видимыми на мобах застрявшие стрелы первым
сделал [Arrow In The Knee](https://modrinth.com/mod/aitk), со случайным расположением из ванили;
этот мод отслеживает реальную точку попадания.
