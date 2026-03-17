package com.peteraraujo.cronx

object ApiRoutes {
    private const val VERSION_PATH = "/v1"

    const val LOGIN = "$VERSION_PATH/auth/login"

    const val LIBRARY = "$VERSION_PATH/library"
    const val LIBRARY_TAGS = "$LIBRARY/tags"

    const val SCHEDULE = "$VERSION_PATH/schedule"

    const val SETTINGS = "$VERSION_PATH/settings"
    const val PRESETS = "$SETTINGS/presets"
}
