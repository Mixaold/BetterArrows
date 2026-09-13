# Better Arrows

Fabric mod for Minecraft 26.2.

Arrows stay where they hit, stuck in whatever body part they went into — they swing with the arm,
bend with the leg. Vanilla just counts how many arrows are in you and re-rolls a random spot on a
random body part every single frame: you nail a zombie in the head, and the arrow turns up in its
leg. And on mobs vanilla doesn't draw them at all.

What you shoot at matters too. Graze stone and the arrow skips off; off wood you have to come in
twice as flat. Sand swallows it unless it hits almost sideways. Wool, mud and snow never bounce it, ever.
The steeper the hit, the more speed stays in the block. On ice the arrow slides along the surface
instead of getting kicked off it like glass. And every material has its own hit sound and its own
spray of debris.

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

MIT. Mobs getting visible stuck arrows at all was first done by
[Arrow In The Knee](https://modrinth.com/mod/aitk), with vanilla's random placement; this one tracks
the real hit point.

---

# Better Arrows (Русский)

Мод под Fabric для Minecraft 26.2.

Стрела торчит там, куда воткнулась, и держится за ту часть тела, в которую вошла: рука дёрнулась —
стрела с ней, нога согнулась — и стрела гнётся. А ваниль помнит только, сколько стрел в цели, и
каждый кадр заново кидает кубик: случайное место на случайной части тела. Всадил зомби в голову —
глядь, стрела в ноге. А на мобах ваниль их и вовсе не рисует.

Важно и то, во что ты стреляешь. Стрельни по камню вскользь, почти вдоль стены — стрела чиркнёт и
улетит дальше. С деревом так же, только стрелять надо ещё ровнее. Песок стрелу проглотит, если она пришла не
совсем уж плашмя. Шерсть, грязь и снег не отбрасывают её вообще никогда. Чем прямее попал, тем
сильнее стрела вязнет в блоке. По льду она едет вдоль поверхности, а не отскакивает, как от стекла.
И у каждого материала свой звук удара и своя крошка.

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

MIT. Видимыми на мобах застрявшие стрелы первым сделал
[Arrow In The Knee](https://modrinth.com/mod/aitk), со случайным расположением из ванили; этот
отслеживает реальную точку попадания.
