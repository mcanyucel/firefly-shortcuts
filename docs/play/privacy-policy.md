# Privacy Policy: Firefly Shortcuts

Effective date: November 5, 2026

Firefly Shortcuts is an open-source Android app by Mustafa Can Yücel, published on Google Play and at https://github.com/mcanyucel/firefly-shortcuts. It is a client for [Firefly III](https://firefly-iii.org/), a self-hosted personal finance manager. It isn't made by or affiliated with the Firefly III project.

This policy covers the official builds from Google Play and the GitHub releases. Modified versions built by others may behave differently.

## Summary

- The app only talks to the Firefly III server **you** enter. It sends nothing to the developer or to anyone else.
- It has no analytics, no advertising, no crash reporting and no tracking.
- What it stores stays on your phone.

## Data the app handles

**Your Firefly III server address and sign-in.** You enter your server's address and either a personal access token or sign in with OAuth. The tokens are stored on your phone, encrypted with a key kept in Android's Keystore, and are only sent to your server to authorize requests.

**Your financial data.** To let you build shortcuts, the app downloads the names of your accounts, categories, budgets, bills, piggy banks and tags from your server. When you run a shortcut, the app sends that transaction (amount, accounts, date, and the category, budget, bill, piggy bank, tags and description you chose) to your server. The shortcuts you create and the downloaded lists are stored on your phone.

**Bank SMS (GitHub build only).** If you allow it, the GitHub build reads incoming SMS to turn your bank's messages into transactions. Each SMS is checked once, on your phone, against the templates you made. A message that no template recognises is dropped and never stored. For one that is recognised, the app keeps only what the template read (amount, currency, date, merchant and the last digits of the card) until it's sent to your server or you discard it. The message you build a template from is stored with that template. The Google Play build doesn't ask for SMS access and can't read SMS.

**Nothing else.** Apart from the SMS above, the app doesn't read your contacts, location, files or any other data on your phone, and it doesn't collect device identifiers.

## Where the data goes

All network traffic goes to the Firefly III server you configured, over HTTPS. The app refuses servers that aren't reached over HTTPS. The developer doesn't run any server the app reports to, and receives none of your data.

When you sign in with OAuth, the app opens your browser at your Firefly III server. After you sign in, the server sends the browser to `https://fireflyiiishortcuts.mustafacanyucel.com/oauth2redirect.html`. Normally Android opens the app directly at that point. If it can't, the browser loads that small page, which passes the sign-in back to the app. The sign-in code in that address only works once and only together with a secret that never leaves the app, so it is useless to anyone else.

## Backups

If Android backup is turned on for your phone, Android may include the app's shortcuts, SMS templates and settings in your Google backup, under your own Google account. Your sign-in tokens can't be read from a backup, because the key that encrypts them never leaves the phone. After restoring onto a new phone, you sign in again.

## Keeping or deleting your data

Everything the app stores is on your phone. To delete it, uninstall the app or clear its storage in Android's settings. Connecting the app to a different server or Firefly III user also deletes the shortcuts, lists and unsent bank messages from the previous one; SMS templates are kept.

Data on your Firefly III server, including transactions the app created, is managed in Firefly III and isn't affected by uninstalling the app.

## Children

The app is a tool for managing personal finances and isn't directed at children.

## Changes to this policy

If this policy changes, the new version is published here with a new effective date. Significant changes are also mentioned in the release notes.

## Contact

Questions about this policy or the app: open an issue at https://github.com/mcanyucel/firefly-shortcuts/issues or email CONTACT_EMAIL.
