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

What you shoot at matters too. Graze stone and the arrow skips off; off wood you have to come in
twice as flat. Sand swallows it unless it hits almost sideways. Wool, mud and snow never bounce it,
ever. The steeper the hit, the more speed stays in the block. On ice the arrow slides along the
surface instead of getting kicked off it like glass. And every material has its own hit sound and
its own spray of debris.

| What you shoot | What happens |
|---|---|
| Stone, metal, glass, deepslate | Skips off, even from a fairly straight hit |
| Wood, bamboo, ladders | Skips off too, but you have to shoot flatter |
| Sand, gravel, dirt | Only if it came in razor flat — any straighter and it's swallowed |
| Wool, mud, snow, slime, cobweb | Never bounces, the arrow just sticks |
| Ice | Doesn't bounce — slides along and slowly stops |
| Leaves | Like vanilla: the arrow just stands in them |

Works on players and on any mob, including mobs from other mods. Rejoin and the arrows are still
there, and other players see them too.

**Settings: Mod Menu → Better Arrows.** Anything can be switched off on its own.

> ### ⚠️ Install it on the client AND the server
> Same version on both sides. Nothing crashes if they differ — the mod just quietly does nothing.
> If a friend sees no arrows, they've got a different version.

Fabric · Minecraft 26.2 · Fabric API · Cloth Config · Mod Menu (optional, for the settings button)

MIT — use it, fork it, put it in your modpack. Arrows on mobs at all were first done by
[Arrow In The Knee](https://modrinth.com/mod/aitk), just not where you actually hit.

---
---

# ЧАСТЬ 3. Описание (Русский)

## Better Arrows

Стрела торчит там, куда воткнулась, и держится за ту часть тела, в которую вошла: рука дёрнулась —
стрела с ней, нога согнулась — и стрела гнётся. А ваниль помнит только, сколько стрел в цели, и
каждый кадр заново кидает кубик: случайное место на случайной части тела. Всадил зомби в голову —
глядь, стрела в ноге. А на мобах ваниль их и вовсе не рисует.

Важно и то, во что ты стреляешь. Стрельни по камню вскользь, почти вдоль стены — стрела чиркнёт и
улетит дальше. С деревом так же, только стрелять надо ещё ровнее. Песок стрелу проглотит, если она
пришла не совсем уж плашмя. Шерсть, грязь и снег не отбрасывают её вообще никогда. Чем прямее попал,
тем сильнее стрела вязнет в блоке. По льду она едет вдоль поверхности, а не отскакивает, как от
стекла. И у каждого материала свой звук удара и своя крошка.

| По чему стреляешь | Что будет |
|---|---|
| Камень, металл, стекло, глубинный сланец | Отскочит, даже если попал довольно прямо |
| Дерево, бамбук, лестницы | Тоже отскочит, но стрелять надо ровнее |
| Песок, гравий, земля | Отскочит, только если прошла впритирку. Чуть прямее — проглотит |
| Шерсть, грязь, снег, слизь, паутина | Не отскакивает никогда, стрела просто втыкается |
| Лёд | Не отскакивает, а едет по льду и постепенно тормозит |
| Листва | Как в обычной игре: стрела торчит в листьях |

Работает на игроках и на любых мобах, хоть из других модов. Перезашёл — стрелы на месте, и другие
игроки их тоже видят.

**Настройки: Mod Menu → Better Arrows.** Что угодно можно выключить по отдельности.

> ### ⚠️ Ставить и на клиент, и на сервер
> Версия одна и та же с обеих сторон. Ничего не упадёт, если они разные, — мод просто молча ничего
> не сделает. Если друг не видит стрел, у него другая версия.

Fabric · Minecraft 26.2 · Fabric API · Cloth Config · Mod Menu (по желанию, ради кнопки настроек)

Лицензия MIT — пользуйтесь, форкайте, кладите в сборки. Стрелы на мобах вообще первым показал
[Arrow In The Knee](https://modrinth.com/mod/aitk) — правда, куда попало.
