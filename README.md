# AZenith banners

This branch is for image assets. The promotional artwork lives here instead of on
`main`, so a normal clone of the module repository stays small. The branch has no
history and none of it is part of the flashable zip or the companion app.

The banner that actually gets installed on device is `mainfiles/module.banner.jpg`
on `main`.

The files are stored as AVIF. If you re-encode them, remember that `--min` and
`--max` are quantizer values rather than quality percentages: lower means better
quality and a larger file.

## Fetching one banner

Over plain HTTPS, without cloning:

```sh
curl -L -o banner.avif \
  https://raw.githubusercontent.com/Liliya2727/AZenith/banners/banner/AZenithBanner4.X.avif
```

With `git`, shallow and without the module history:

```sh
git clone --depth 1 --single-branch --branch banners \
  https://github.com/Liliya2727/AZenith.git ~/az-banners
```

Or out of an existing clone, which transfers only the banner branch:

```sh
git fetch origin banners
git checkout FETCH_HEAD -- banner/
```

## Licence

Same Apache-2.0 terms as the rest of the project. See `LICENSE` on `main`.
