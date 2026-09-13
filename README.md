# Better Arrows

Fabric mod for Minecraft 26.2.

Arrows stick where they actually hit, and stay on the limb they hit — swinging with the arm, bending
with the leg. Vanilla only remembers *how many* arrows are in something and re-rolls a random spot
on a random body part every frame: shoot a zombie in the head and the arrow turns up in its leg. On
mobs it draws nothing at all.

Blocks answer back too. A glancing shot skips off stone, needs about half that angle to skip off
wood, and is swallowed by sand unless it arrives almost flat — wool, mud and snow never throw an
arrow off at all. The steeper the hit, the more of its speed stays in the block. On ice an arrow
skates *along* the surface instead of pinging off it like a window. Every material has its own
impact sound and throws off crumbs of its own texture.

Each part can be switched off on its own.

## Install

Needs **Fabric API** and **Cloth Config**. Mod Menu is optional — it just adds the settings button.

Put the jar on **both the client and the server**, same version on both sides: the client draws the
arrows, the server decides where they landed and whether they bounced.

Settings: Mod Menu → Better Arrows.

## Build

```
./gradlew build
```

The jar lands in `build/libs/` — the plain one, not `-sources`.

## License

MIT.

Positioning research drew on vanilla's own stuck-arrow rendering and on
[Arrow In The Knee](https://modrinth.com/mod/aitk) by RazorPlay01, which first gave mobs visible
stuck arrows, keeping vanilla's random placement.

---

# Better Arrows (Русский)

Мод под Fabric для Minecraft 26.2.

Стрелы торчат там, куда попали, и остаются на той части тела, в которую вошли — качаются вместе с
рукой, гнутся вместе с ногой. Ваниль помнит только *сколько* стрел в цели и каждый кадр заново
разыгрывает случайное место на случайной части тела: стреляешь зомби в голову — стрела оказывается в
ноге. А на мобах ваниль не рисует их вовсе.

Блоки тоже отвечают. Пологий выстрел отскакивает от камня, от дерева — вдвое более пологий, а песок
глотает стрелу, если она пришла не почти плашмя; шерсть, грязь и снег не отбрасывают её никогда. Чем
круче удар, тем больше скорости остаётся в блоке. По льду стрела едет *вдоль* поверхности, а не
пинается от неё, как от окна. У каждого материала свой звук попадания и своя крошка.

Каждую часть можно выключить отдельно.

## Установка

Нужны **Fabric API** и **Cloth Config**. Mod Menu по желанию — он только добавляет кнопку настроек.

Ставить jar **и на клиент, и на сервер**, одной и той же версии: клиент рисует стрелы, сервер решает,
куда они попали и отскочили ли.

Настройки: Mod Menu → Better Arrows.

## Сборка

```
./gradlew build
```

Готовый jar появится в `build/libs/` — обычный, не `-sources`.

## Лицензия

MIT.

В основе позиционирования — открыто задокументированное поведение ванильного рендера застрявших
стрел и мод [Arrow In The Knee](https://modrinth.com/mod/aitk) авторства RazorPlay01, который первым
сделал застрявшие стрелы видимыми на мобах (со случайным расположением из ванили).
