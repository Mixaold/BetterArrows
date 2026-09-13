# Better Arrows — page copy for Modrinth

Paste the English half into the project description (Modrinth pages are English-first). The Russian
half below is the same text, for your own use — Modrinth has no multi-language project pages.

Summary field (~150 chars):
*Arrows stick exactly where they hit and follow the animated limb — and blocks answer them by material, with glancing ricochets and arrows skating across ice.*

---

# Better Arrows

Arrows that hit a mob or a player stick **exactly where they landed** and follow the animated limb —
instead of vanilla's random placement. And blocks stop being one generic wall: a glancing shot skips
off stone, sand swallows it, ice sends it skating along the surface.

## The problem it started with

In vanilla, a stuck arrow's position has nothing to do with where it actually hit. The game
remembers only *how many* arrows are stuck; every frame it re-rolls a random spot on a random body
part, seeded by the entity's id. Shoot a zombie in the head and the arrow can show up in its leg.
Mobs do not even get that much — vanilla never draws stuck arrows on them at all.

## Arrows stick where they hit

- **The real hit point** — the mod captures the exact 3D point vanilla itself computes on impact and
  then throws away, and anchors the arrow there.
- **Follows the right bone** — the arrow belongs to the body part it hit, so it swings with an arm,
  bends with a leg and stays put through walking, attacking and flying, rather than being glued to
  the entity's overall position.
- **Real trajectory, not a guess** — the arrow's angle comes from its actual flight direction, so a
  shot from below looks like a shot from below.
- **Every living entity** — players and mobs, vanilla and modded, with no per-entity allowlist.
- **Sensible edge cases** — arrows do not balloon on a giant slime or shrink out of sight on a baby
  mob, they do not stick to endermen (vanilla never lets them either), and they land on the model's
  real surface even where the hitbox is looser than the mesh, as on an iron golem.
- **Survives a rejoin** — the hit is stored relative to the entity and persisted, so a player who
  joins later sees the arrows already stuck, in the same places.
- **Fades out like vanilla** — arrows work their way out as vanilla's arrow count decays, oldest
  first, so a heavily shot mob never accumulates them forever.

## Blocks answer back

- **Ricochet.** A glancing shot skips off instead of planting itself, and the material decides both
  how shallow it has to be and how much speed survives. Stone takes a hit well off the perpendicular
  (up to 35° from the surface by default), wood about half that, loose ground like sand, gravel and
  dirt only a sliver near flat — and wool, mud, slime or snow never throw an arrow off at all. The
  steeper the hit, the more of its energy stays in the block. Leaves keep vanilla's answer: an arrow
  standing in a canopy is what everyone expects to see.
- **Ice.** A shallow shot skates *along* the ice, slowing as it goes, instead of pinging off it like
  a window — the game tells mods that ice is glass, which is why nothing else does this. An arrow
  can never bury itself in ice either: a steep shot is thrown off rather than embedded, again and
  again until it is spent and simply lies there. Which blocks count is the block tag
  `betterarrows:arrows_slide_on`, `#minecraft:ice` by default, so a datapack can add more.
- **Material impact sounds.** Wood thuds, stone clacks, metal rings, replacing the one generic
  arrow thunk. Deliberately quieter than vanilla's — a detail you notice standing next to the arrow,
  not an announcement. Blocks that already make their own noise, like bells and amethyst, are left
  alone (`betterarrows:no_impact_sound`).
- **Block debris.** Crumble particles carrying the real texture of the block that was hit, scaled by
  how hard the arrow arrived.

No damage tuning anywhere: vanilla already scales arrow damage by speed, so an arrow that shed energy
on a bounce hits softer on its own, and one that ricochets off a ceiling and accelerates on the way
down hits harder.

## Settings

Everything above can be switched off on its own, in `config/betterarrows.json` or through the
in-game screen (English and Russian) if you have Mod Menu. On a server, the server's file decides
gameplay — ricochet, ice, sounds, particles — and each client's file decides rendering.

## Fresh Animations and other model replacements

Works with them, and was built for it — but they change what "where it hit" means.

Packs like **Fresh Animations** (through Entity Model Features) do not tweak the vanilla model, they
replace it: their own bones, their own animations, and an inflated shell layered over every limb.
Two things follow.

Arrows attach to the **bone**, never to that shell. The shell is the nearest surface to an incoming
arrow, so the obvious "closest piece of model" answer picks it every time — and it is the wrong one,
because a shell is animated by the pack and need not move with the bone at all. An arrow stuck to it
sits in the head and then refuses to turn with it.

And a replaced mesh is simply not where the hitbox is. The mod allows the gap, up to three quarters
of a block; past that it draws nothing rather than guessing. So on a heavily reshaped model the
occasional arrow will be missing rather than misplaced. That is deliberate.

## Requirements

- Minecraft 26.2, Fabric Loader
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Cloth Config](https://modrinth.com/mod/cloth-config) — required; it draws the settings screen
- [Mod Menu](https://modrinth.com/mod/modmenu) — optional, it only adds the button that opens that
  screen
- Needed on **both client and server** (or on singleplayer's integrated server): the client draws
  the arrows, the server decides where they landed and whether they bounced

## Known limitations

- The hit point is stored relative to the entity, so *where on the body* an arrow sits survives a
  rejoin. What is not stored is the skeletal pose at the moment of impact: an arrow first drawn
  while the mob happens to be mid-stride can settle on a slightly different spot of a moving limb
  than it would have at rest.
- Mobs whose model structure changes with their state — a shulker opening and closing, say — are
  not specially handled.
- A resource pack that replaces an entity's model entirely can move where an arrow ends up
  anchored, since the body part it was resolved against may no longer exist.

## Credits

Positioning research drew on the publicly documented behaviour of vanilla's own stuck-arrow
rendering and on ["Arrow In The Knee"](https://modrinth.com/mod/aitk) by RazorPlay01, which first
extended stuck-arrow *visibility* to mobs, keeping vanilla's random placement — this mod goes
further and tracks the real hit point instead.

---

# Better Arrows (Русский)

Стрелы, попавшие в моба или игрока, торчат **именно там, куда вошли**, и следуют за анимированной
конечностью, вместо случайного расположения из ванили. А блоки перестают быть одной безликой стеной:
пологий выстрел отскакивает от камня, песок стрелу глотает, а по льду она уезжает вдоль поверхности.

## С чего всё началось

В ваниле положение застрявшей стрелы никак не связано с реальным местом попадания. Игра помнит
только *количество* стрел, а место каждый кадр разыгрывает заново — случайную точку на случайной
части тела. Выстрелил зомби в голову — стрела может оказаться в ноге. Да и то лишь для игроков: на
мобах ваниль застрявшие стрелы вообще не рисует.

## Стрелы торчат там, куда попали

- **Реальная точка попадания** — мод перехватывает точную 3D-координату, которую ваниль сама
  вычисляет при попадании и тут же забывает, и крепит стрелу именно туда.
- **Следует за правильной костью** — стрела принадлежит той части тела, в которую вошла, поэтому
  качается вместе с рукой, гнётся с ногой и остаётся на месте при ходьбе, атаке и полёте, а не
  «приклеена» к общему положению сущности.
- **Реальная траектория, а не догадка** — наклон стрелы берётся из настоящего направления полёта,
  поэтому выстрел снизу вверх так и выглядит.
- **Любая живая сущность** — игроки и мобы, ванильные и модовые, без списка исключений.
- **Разумные крайние случаи** — стрелы не раздуваются на гигантском слизне и не теряются на
  детёныше, не застревают на эндермене (в ваниле это тоже невозможно) и садятся на реальную
  поверхность модели даже там, где хитбокс шире меша, как у железного голема.
- **Переживают перезаход** — попадание хранится относительно самой сущности, поэтому зашедший позже
  игрок увидит уже воткнутые стрелы, и ровно там же.
- **Исчезают как в ваниле** — стрелы выходят из моба по мере убывания ванильного счётчика, начиная с
  самых старых, так что у многократно обстрелянного моба они не копятся бесконечно.

## Блоки отвечают

- **Рикошет.** Пологий выстрел отскакивает, а не втыкается, и материал решает и то, насколько
  пологим он должен быть, и то, сколько скорости уцелеет. Камень принимает удар далеко от
  касательного (по умолчанию до 35° от поверхности), дерево — примерно вдвое более пологий, сыпучие
  блоки вроде песка, гравия и земли — лишь узкую полоску у самой плоскости, а шерсть, грязь, слизь и
  снег не отбрасывают стрелу никогда. Чем круче удар, тем больше энергии остаётся в блоке. Листва
  ведёт себя по-ванильному: стрела, торчащая в кроне, — ровно то, что все и ожидают увидеть.
- **Лёд.** Пологий выстрел едет *вдоль* льда, постепенно замедляясь, а не пинается от него, как от
  окна: игре лёд представляется стеклом, поэтому больше так не умеет никто. Воткнуться в лёд стрела
  тоже не может — слишком крутой выстрел отбрасывает, и ещё раз, и ещё, пока она не выдохнется и не
  ляжет на поверхность. Какие блоки считаются скользкими, задаёт тег
  `betterarrows:arrows_slide_on`, по умолчанию `#minecraft:ice`, — датапак может дополнить.
- **Звук по материалу.** Дерево стучит, камень щёлкает, металл звенит — вместо одного общего «тук».
  Намеренно тише ванильного: это деталь, которую слышно рядом, а не событие на всю округу. Блоки со
  своим голосом, вроде колокола и аметиста, не трогаются (`betterarrows:no_impact_sound`).
- **Крошка от блока.** Частицы с настоящей текстурой блока, в который попали, тем гуще, чем сильнее
  прилетело.

Урон нигде не подкручивается: ваниль и так считает его от скорости, поэтому потерявшая на отскоке
энергию стрела бьёт слабее сама, а срикошетившая от потолка и разогнавшаяся вниз — сильнее.

## Настройки

Всё перечисленное выключается по отдельности — в `config/betterarrows.json` или через внутриигровой
экран (русский и английский), если стоит Mod Menu. На сервере серверный файл решает геймплей —
рикошет, лёд, звуки, частицы, — а файл каждого клиента решает отрисовку.

## Fresh Animations и другие замены моделей

Работает, и делалось с оглядкой на них — но они меняют сам смысл слов «куда попала стрела».

Паки вроде **Fresh Animations** (через Entity Model Features) не правят ванильную модель, а заменяют
её целиком: свои кости, своя анимация и раздутая оболочка поверх каждой конечности. Отсюда две вещи.

Стрела цепляется к **кости**, а не к этой оболочке. Оболочка — ближайшая к прилетающей стреле
поверхность, поэтому очевидный ответ «ближайший кусок модели» всегда выбирает именно её, и это
неверный выбор: оболочку анимирует пак, и с костью она двигаться не обязана. Стрела, прицепленная к
ней, торчит в голове и не поворачивается вместе с ней.

И заменённая меш просто не совпадает с хитбоксом. Мод допускает расхождение до трёх четвертей блока,
дальше — не рисует ничего вместо того, чтобы гадать. Так что на сильно перекроенной модели отдельная
стрела скорее не появится, чем встанет не туда. Это сделано намеренно.

## Требования

- Minecraft 26.2, Fabric Loader
- [Fabric API](https://modrinth.com/mod/fabric-api)
- [Cloth Config](https://modrinth.com/mod/cloth-config) — обязателен, он рисует экран настроек
- [Mod Menu](https://modrinth.com/mod/modmenu) — по желанию, он лишь добавляет кнопку к этому экрану
- Нужен **и на клиенте, и на сервере** (либо на встроенном сервере одиночной игры): клиент рисует
  стрелы, сервер решает, куда они попали и отскочили ли

## Известные ограничения

- Точка попадания хранится относительно самой сущности, поэтому место на теле переживает перезаход.
  Что не сохраняется — поза скелета в момент попадания: стрела, впервые отрисованная, когда моб как
  раз в движении, может закрепиться на движущейся конечности чуть иначе, чем закрепилась бы в покое.
- Мобы, у которых структура модели меняется вместе с состоянием (например, шалкер, который
  открывается и закрывается), отдельно не обрабатываются.
- Ресурспак, полностью заменяющий модель сущности, может сместить точку крепления: той части тела, к
  которой стрела была привязана, может уже не существовать.

## Благодарности

Исследование позиционирования опиралось на открыто задокументированное поведение ванильного рендера
застрявших стрел и на мод ["Arrow In The Knee"](https://modrinth.com/mod/aitk) авторства RazorPlay01,
который первым расширил ВИДИМОСТЬ застрявших стрел на мобов (со случайным расположением из ванили) —
этот мод идёт дальше и отслеживает реальную точку попадания.
