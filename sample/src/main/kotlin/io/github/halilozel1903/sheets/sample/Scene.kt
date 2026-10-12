package io.github.halilozel1903.sheets.sample

/** Screenshot scenes, picked with the `scene` intent extra. */
enum class Scene(val key: String) {
    Peek("peek"),
    Half("half"),
    Full("full"),
    Keyboard("keyboard"),
    Tablet("tablet"),
    ;

    companion object {
        fun from(key: String?): Scene? = entries.firstOrNull { it.key == key }
    }
}
