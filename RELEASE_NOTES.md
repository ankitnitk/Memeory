# Release Notes

Newest changes at the top. Written in plain language.

---

## Test build 1.0.0: reading and searching memes (20 Oct 2026)

**New**
- **Memeory reads the words in your memes.** After you add memes, it reads the text printed on each one in the background, with no internet. A banner shows how many are left. New memes get their preview first, so they appear in the grid right away.
- **Search as you type.** The search bar at the top finds memes by the words in them. Partial words work: "mond" finds "Monday". Several words find memes that have all of them. Upper or lower case doesn't matter.
- **Meme detail screen.** Tap a meme to see it big (GIFs play), the text Memeory found in it (long-press to copy words), where it's kept, its size and when it was added.
- **Missing originals.** If a folder meme was deleted from your phone, the detail screen says so and shows the saved preview instead.
- The Back button clears the search before leaving the app.

**Good to know**
- Text reading works for languages written in English letters (English, Swahili, Sheng, Hinglish). Hindi in Devanagari script comes later.
- If you installed the first test build, memes saved in it are cleared once. This happens only for test builds; future updates will always keep your memes.
- Android now lists "view network connections" for Memeory. It can't send anything, and the app still has no internet permission. The README explains why it's needed.

---

## Test build 1.0.0: saving memes (20 Oct 2026)

**New**
- **Save from any app.** Share an image (photo, GIF or WebP) from WhatsApp, Chrome, Instagram, Reddit and others, and pick **Save to Memeory**. A small card says "Saved!" and closes by itself. You can share several at once.
- **Pick from your gallery.** Tap **Add memes → From your gallery** and choose as many as you like. Memeory doesn't need permission to see all your photos. You only give it the ones you pick.
- **Add a folder.** Tap **Add memes → Add a folder** and choose a folder such as WhatsApp Images. Memeory finds every image in it, including sub-folders, without making copies. Each time you open the app, it checks the folder for new memes.
- **Meme grid.** All your memes in one scrolling grid, newest first. GIFs show a small "GIF" label.
- **No double saves.** Sharing the same meme twice keeps only one copy.
- **Missing memes are marked.** If a meme from a folder is deleted from your phone, Memeory shows a 🔗 badge on it instead of failing. It clears if the file comes back.
- **Light and dark mode** follow your phone's setting.

**Privacy**
- The app has no internet permission. Every build is checked automatically to make sure.
- Memeory's data is not uploaded to Google's cloud backup. When you set up a new Android 12+ phone with a cable or direct phone-to-phone transfer, your memes can come along.

**Behind the scenes**
- The app's permanent Play Store ID is `com.memeoryapp.android`, because `com.memeory.app` was already taken. Test builds use `com.memeoryapp.android.debug`, so they can sit next to the Play Store version.

**Not yet**
- Tapping a meme does nothing yet. Searching, sharing memes back out, notes and tags come in the next phases.

---

## Not released yet: getting started (20 Oct 2026)

**What happened**
- Looked at the Google Play Store to see which similar meme apps already exist. Several do (MemeOrganizer, Mimfold, MemeScanner, Meme Library, MEME-Finder). None of them lets you search by *"use this when..."* notes, and none of them makes a point of having no internet access at all. Those are Memeory's main differences.
- Because one competitor already has a meme keyboard, the Memeory keyboard moved up into the first version instead of waiting for version 2.
- Created this project with its README and these release notes.

**What you can do with it**
- Nothing to install yet. The app itself comes next.
