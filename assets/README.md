# assets/

Binary images (webp, png) for the whole client, grouped by resource type (`images/`), then by owning module:

```
assets/
└── images/<module>/drawable-nodpi/   raster images (webp, png)
```

Android only packages resources a module declares, so each module's `build.gradle.kts` adds its folder here
as an extra `res` source directory (`androidComponents { onVariants { it.sources.res?.addStaticSourceDirectory(...) } }`). Resource names,
the `<module>_` prefix and the generated `R` classes are unchanged, so no code refers to the new location.

Rules:
- Keep the per-module subfolder: pointing two modules at one shared folder would package every file into both.
- Keep the standard Android directory name (`drawable-nodpi`, ...) inside each module folder.
- Vector icons (xml drawables), strings, colors and themes stay in each module's `src/main/res`.
- Other resource types would get their own sibling folder (`assets/fonts/`, ...) wired the same way.
- A new image: drop it in the owning module's folder; a new module with images needs the same `srcDir` line.
