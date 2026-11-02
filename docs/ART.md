# Training Arc Art

Ember is an original flame spirit, not an existing character. Use bold dark
outlines, flat cel-style colors, a single highlight, readable eyes, and small
spark accents. The orange/cyan/pink palette follows both app themes. Resting
expressions replace punitive or broken-streak imagery. Barlow remains the
bundled OFL typeface.

## Provenance

| Asset | Origin | Date | Rights |
| --- | --- | --- | --- |
| Ember stages and five expressions | Original Compose Canvas paths created for NewMe | 2026-11-01 | Original project artwork; no third-party character material |
| Sunrise, midday, sunset, night, comeback, celebration, empty scenes | Original procedural Compose Canvas scenes | 2026-11-01 | Original project artwork |
| Mood faces, water bottle, sleep moon, emblem shapes | Original Compose Canvas geometry | 2026-11-01 | Original project artwork |
| Activity and emblem glyphs | AndroidX Material icons, existing dependency | 2026-11-01 | Apache-2.0 Material icon library; not claimed as original characters |
| Barlow | Existing bundled fonts | Existing assets | SIL Open Font License; retain the bundled licence |

## Poster Slots

PosterImage resolves `poster_welcome`, `poster_morning`, `poster_midday`,
`poster_evening`, `poster_night`, `poster_comeback`, `poster_levelup`,
`poster_empty_routines`, and `poster_empty_progress`. Missing raster files use
the local Canvas fallback, so no slot requires a network request.

Optional replacements belong under `app/src/main/res/drawable-nodpi/` as
`poster_<slot>.webp`. Use original portrait 4:5 artwork, no text/logos/existing
characters, and at most 250 KB per image. Record the generator/tool, date,
licence/terms, and source for every replacement in this table before bundling.
No generated raster replacements are currently included.

Artwork is decorative unless it communicates a state; all state-bearing art
has TalkBack labels. Motion is disabled by LocalReducedMotion. Touch controls
are at least 48dp. Raster artwork must not increase the APK by more than the
Phase 2 budget of 10 MB.