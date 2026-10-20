# Memeory 🧠😂

**The right meme, right on cue.**

Memeory (meme + memory) is a free Android app that keeps your favourite memes in one place, so you can find the right one in seconds when a chat needs it.

> 🚧 **Status: being built.** An early test version is available (see *Try the test version* below). See [RELEASE_NOTES.md](RELEASE_NOTES.md) for progress.

---

## The problem it solves

You save a funny meme. Months later, a friend says something and that meme would be the perfect reply. But either you forgot you had it, or it's lost somewhere among thousands of photos in your gallery.

Memeory fixes that. You save memes into it once, and later you just type a word or two to find them.

## What it will do

- **Save memes easily.** Share an image from WhatsApp, Chrome, Instagram, Reddit or any other app straight into Memeory. You can also pick many images at once from your gallery, or point it at a folder like *WhatsApp Images* or *Downloads*.
- **Read the words in each meme.** Memeory reads the text printed on the image, so you can search for it later. This happens on your phone, with no internet.
- **"Use this when..." notes.** You can add a short note such as *"when someone is late"* or *"reply to 'where are you'"*, so you can search by the situation, not only by the words in the picture. Notes are optional, so saving still takes one tap.
- **Instant search.** Results show up as you type. Typing *"mond"* already finds *"Monday"*. Memes you use often and your favourites come first.
- **Send it back fast.** Tap a meme, then **Share**, **Send to WhatsApp**, or **Copy**.
- **Meme keyboard.** Search your memes and send one without leaving the chat. *(Planned)*
- **Quick access.** A home-screen widget and a Quick Settings button open Memeory in one tap. *(Planned)*
- **Favourites and tags** to keep things tidy.
- **Full backup.** Export everything (images, notes, tags) to a single ZIP file, and import it back on any phone. Your memes are never locked inside the app.

## How Memeory stores your memes

There are two ways memes get in, and they are kept slightly differently:

| How you add it | What Memeory does | What happens if you delete the photo from your gallery |
|---|---|---|
| **Share** it from another app, or **pick** it from your gallery | Keeps its **own copy**, inside the app | Nothing. The meme stays in Memeory. |
| **Add a folder** (like WhatsApp Images) | **Doesn't copy.** Remembers where each meme is, so no extra space is used | The meme is marked with a 🔗 "missing" badge in Memeory |

Why the difference? When another app shares an image, Android only lets Memeory read it for a moment, so it must be copied. A folder you add stays readable, so a copy isn't needed.

Every meme also gets a small preview picture, so the grid scrolls smoothly even with thousands of memes.

## Try the test version

1. On your Android phone, open the [latest test build](https://github.com/ankitnitk/Memeory/releases/tag/latest).
2. Download the `.apk` file and open it. Android will ask you to allow installing apps from your browser. Allow it once.
3. Open Memeory, or share any image from another app and choose **Save to Memeory**.

## Your privacy

- **No internet permission.** The app doesn't ask Android for internet access, so it can't send anything anywhere. Anyone can check this on the app's Play Store page or in Android settings.
- **No account, no login, no server.**
- **No ads, no analytics, no tracking.**
- Everything stays on your phone.

The full privacy policy will be published at `docs/privacy-policy.html`.

## Requirements

- Android 8.0 or newer

## How it's built (for the curious)

- Written in **Kotlin** with **Jetpack Compose** (Google's modern way to build Android screens).
- Text reading uses **Google ML Kit**, packed inside the app so it works offline.
- Memes and their text are stored in a small database on the phone (**Room**), with fast word search.
- The app is built automatically on GitHub (GitHub Actions), so you don't need Android Studio to get an installable file. Each build also checks that the app does **not** have internet permission, and fails if it ever does.
- The code is split in two parts: `app` (the screens) and `core-data` (the meme database, storage and import). The meme keyboard will reuse `core-data`.

## Roadmap

| Phase | What's in it | Status |
|---|---|---|
| 0 | Check which similar apps already exist | ✅ Done |
| 1 | Project setup, saving memes (share, gallery, folders), meme grid | ✅ Done |
| 2 | Reading text in memes, search, meme detail screen | Planned |
| 3 | Share, Send to WhatsApp, Copy, notes, tags, favourites | Planned |
| 4 | Meme keyboard | Planned |
| 5 | Widget, Quick Settings button, backup, welcome screens, settings | Planned |
| 6 | Privacy policy page, signed release, Play Store | Planned |

**Later:** spotting duplicate memes, reading Hindi text, and searching by meaning. All of these will run on the phone too.

## License

To be decided.
