# Cinematic Interactions Compat

Unofficial NeoForge compatibility addon for **Cinematic Interactions 0.18.30** on Minecraft 1.21.1 / NeoForge 21.1.x.

## Supported mods

- **Cinematic Interactions 0.18.30** — required.
- **Easy Magic** — enchanting-table rendering, enchantment clues and reroll interaction.
- **Easy Anvils** — anvil item rendering and scene outro.
- **Goety** — all three Dark Anvil damage stages (normal, chipped and damaged) use the Cinematic Interactions anvil scene; two input slots persist in the block and are rendered on top of the anvil.
- **Carved Wood** — crafting tables, crafters, lecterns, campfires and chiseled bookshelves.
- **Every Compat** — generated Carved Wood chiseled-bookshelf variants and their book textures.
- **Quark** — modded furnaces.
- **Caverns & Chasms** — Bejeweled Anvil.
- **Amendments** — lectern compatibility.
- **Combat Roll** — hides its cooldown HUD widget while a Cinematic Interactions station scene is active.

All mods except Cinematic Interactions are optional. Install only the integrations you need. Compatibility is intended for Minecraft 1.21.1 and NeoForge 21.1.x.

### Goety and Easy Anvils

Goety's Dark Anvil has its own `DarkAnvilMenu`, which extends Minecraft's `AnvilMenu`. The addon recognizes the normal, chipped and damaged Goety blocks as anvil stations, so Cinematic Interactions can use its native anvil scene.

Easy Anvils can remain installed alongside Goety. Its block conversion targets vanilla `AnvilBlock` implementations, while Goety's Dark Anvil is a separate `FallingBlock` implementation. The addon therefore preserves Goety's own menu, repair costs, damage stages and soul-powered repair behavior instead of replacing them with Easy Anvils' menu. The addon adds a persistent two-slot block inventory to the three Dark Anvil stages, keeps the items when the menu closes or the player leaves the world, and drops the stored items when the anvil is broken. The items use the same centered 3D anvil layout used by the Easy Anvils integration. Easy Anvils' repair rebalance is not applied to Goety's Dark Anvil.

### Combat Roll

When Combat Roll is installed, its cooldown indicator and keybind HUD are hidden during any active Cinematic Interactions scene. The rest of the Minecraft HUD remains visible, and Combat Roll's HUD returns when the scene ends.

## Build

Requirements: **64-bit Java 21** and an internet connection for the Gradle bootstrap/dependencies.

On Windows, run `build.bat` or:

```powershell
.\\gradlew.bat clean build
```

The JAR is created in:

```
build\\libs\\Cinematic Interactions-Compat-NeoForge-1.21.1-1.0.0.jar
```

Copy it to your Minecraft instance's `mods` folder together with Cinematic Interactions and any optional supported mods.

## License

MIT. See [LICENSE](LICENSE).

---

# Cinematic Interactions Compat (Русский)

Аддон совместимости для **Cinematic Interactions** на Minecraft 1.21.1 / NeoForge.

## Поддерживаемые моды

- **Cinematic Interactions 0.18.30** — обязательный мод.
- **Easy Magic** — отображение предметов на столе зачарования, подсказки зачарований и повторный выбор.
- **Easy Anvils** — отображение предметов на наковальне и анимация выхода.
- **Goety** — все три состояния тёмной наковальни (обычное, сколотое и повреждённое) используют сцену наковальни Cinematic Interactions; два входных слота сохраняются в блоке и отображаются на модели наковальни.
- **Carved Wood** — верстаки, сборщики, кафедры, костры и резные книжные полки.
- **Every Compat** — созданные Every Compat варианты резных книжных полок Carved Wood и их текстуры книг.
- **Quark** — модифицированные печи.
- **Caverns & Chasms** — украшенная наковальня.
- **Amendments** — совместимость с кафедрами.
- **Combat Roll** — скрытие индикатора перезарядки и клавиши переката на время активной сцены Cinematic Interactions.

Все перечисленные моды, кроме Cinematic Interactions, необязательны. Устанавливайте только нужные интеграции. Совместимость рассчитана на Minecraft 1.21.1 и NeoForge 21.1.x.

### Goety и Easy Anvils

У тёмной наковальни Goety собственное меню `DarkAnvilMenu`, наследующее ванильное `AnvilMenu`. Аддон распознаёт обычную, сколотую и повреждённую наковальни Goety как наковальни, поэтому Cinematic Interactions использует для них штатную сцену.

Easy Anvils можно устанавливать одновременно с Goety. Система замены блоков Easy Anvils предназначена для наследников ванильного `AnvilBlock`, а тёмная наковальня Goety реализована отдельным блоком на основе `FallingBlock`. Поэтому аддон сохраняет собственные меню, стоимость ремонта, стадии износа и восстановление за энергию душ Goety, не подменяя их меню Easy Anvils. Аддон добавляет собственное постоянное хранилище из двух слотов для всех трёх состояний тёмной наковальни: предметы остаются в блоке после закрытия меню и выхода из мира, а при разрушении наковальни выпадают. Для предметов используется такое же центральное 3D-расположение, как в интеграции Easy Anvils. Перебалансировка ремонта Easy Anvils на тёмную наковальню Goety не распространяется.

### Combat Roll

Если установлен Combat Roll, индикатор перекатов и подсказка клавиши скрываются на время любой активной сцены Cinematic Interactions. Остальные элементы HUD Minecraft остаются видимыми. После завершения сцены HUD Combat Roll возвращается.

## Сборка

Требования: **64-битная Java 21** и подключение к интернету для загрузки Gradle и зависимостей.

В Windows запустите `build.bat` или выполните в PowerShell:

```powershell
.\\gradlew.bat clean build
```

Готовый JAR появится здесь:

```
build\\libs\\Cinematic Interactions-Compat-NeoForge-1.21.1-1.0.0.jar
```

Скопируйте его в папку `mods` вашего клиента Minecraft вместе с Cinematic Interactions и нужными дополнительными модами.

## Лицензия

MIT. См. [LICENSE](LICENSE).
