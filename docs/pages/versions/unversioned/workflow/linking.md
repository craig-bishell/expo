---
title: Linking
---

import SnackEmbed from '~/components/plugins/SnackEmbed';

## Introduction

Every good website is prefixed with `https://`, and `https` is what is known as a _URL scheme_. Insecure websites are prefixed with `http://`, and `http` is the URL scheme. Let's call it scheme for short.

To navigate from one website to another, you can use an anchor tag (`<a>`) on the web. You can also use JavaScript APIs like `window.history` and `window.location`.

In addition to `https`, you're likely also familiar with the `mailto` scheme. When you open a link with the `mailto` scheme, your operating system will open an installed mail application. If you have more than one mail application installed then your operating system may prompt you to pick one. Similarly, there are schemes for making phone calls and sending SMS'. Read more about [built-in URL schemes](#built-in-url-schemes) below.

`https` and `http` are handled by your browser, but it's possible to link to other applications by using different url schemes. For example, when you get a "Magic Link" email from Slack, the "Launch Slack" button is an anchor tag with an href that looks something like: `slack://secret/magic-login/other-secret`. Like with Slack, you can tell the operating system that you want to handle a custom scheme. Read more about [configuring a scheme](#in-a-standalone-app). When the Slack app opens, it receives the URL that was used to open it and can then act on the data that is made available through the url -- in this case, a secret string that will log the user in to a particular server. This is often referred to as **deep linking**. Read more about [handling deep links into your app](#handling-links-into-your-app).

Deep linking with scheme isn't the only linking tool available to you. It is often desirable for regular HTTPS links to open your application on mobile. For example, if you're sending a notification email about a change to a record, you don't want to use a custom URL scheme in links in the email, because then the links would be broken on desktop. Instead, you want to use a regular HTTPS link such as `https://www.myapp.io/records/123`, and on mobile you want that link to open your app. iOS terms this concept "universal links" and Android calls it "deep links"; Expo supports these links on both platforms (with some [configuration](#universaldeep-links-without-a-custom-scheme)). Expo also supports deferred deep links with [Branch](../../sdk/branch/).

## Linking from your app to other apps

### Built-in URL Schemes

As mentioned in the introduction, there are some URL schemes for core functionality that exist on every platform. The following is a non-exhaustive list, but covers the most commonly used schemes.

| Scheme           | Description                                  | iOS | Android |
| ---------------- | -------------------------------------------- | --- | ------- |
| `mailto`         | Open mail app, eg: `mailto: support@expo.io` | ✅  | ✅      |
| `tel`            | Open phone app, eg: `tel:+123456789`         | ✅  | ✅      |
| `sms`            | Open SMS app, eg: `sms:+123456789`           | ✅  | ✅      |
| `https` / `http` | Open web browser app, eg: `https://expo.io`  | ✅  | ✅      |

### Opening links from your app

There is no anchor tag in React Native, so we can't write `<a href="https://expo.io">`, instead we have to use `Linking.openURL`.

```javascript
import { Linking } from 'react-native';

Linking.openURL('https://expo.io');
```

Usually you don't open a URL without it being requested by the user -- here's an example of a simple `Anchor` component that will open a URL when it is pressed.

```javascript
import { Linking, Text } from 'react-native';

export default class Anchor extends React.Component {
  _handlePress = () => {
    Linking.openURL(this.props.href);
    this.props.onPress && this.props.onPress();
  };

  render() {
    return (
      <Text {...this.props} onPress={this._handlePress}>
        {this.props.children}
      </Text>
    );
  }
}

// <Anchor href="https://google.com">Go to Google</Anchor>
// <Anchor href="mailto://support@expo.io">Email support</Anchor>
```

### Using `Expo.WebBrowser` instead of `Linking` for opening web links

The following example illustrates the difference between opening a web link with `Expo.WebBrowser.openBrowserAsync` and React Native's `Linking.openURL`. Often `WebBrowser` is a better option because it's a modal within your app and users can easily close out of it and return to your app.

<SnackEmbed snackId="H11a8rk7b" />

### Opening links to other apps

If you know the custom scheme for another app you can link to it. Some services provide documentation for deep linking, for example the [Lyft deep linking documentation](https://developer.lyft.com/v1/docs/deeplinking) describes how to link directly to a specific pickup location and destination:

```
lyft://ridetype?id=lyft&pickup[latitude]=37.764728&pickup[longitude]=-122.422999&destination[latitude]=37.7763592&destination[longitude]=-122.4242038
```

It's possible that the user doesn't have the Lyft app installed, in which case you may want to open the App / Play Store, or let them know that they need to install it first. We recommend using the library [react-native-app-link](https://github.com/fiber-god/react-native-app-link) for these cases.

On iOS, `Linking.canOpenURL` requires additional configuration to query other apps' linking schemes. You can use the `ios.infoPlist` key in your `app.json` to specify a list of schemes your app needs to query. For example:

```
  "infoPlist": {
    "LSApplicationQueriesSchemes": ["lyft"]
  }
```

If you don't specify this list, `Linking.canOpenURL` may return `false` regardless of whether the device has the app installed. Note that this configuration can only be tested in standalone apps, because it requires native changes that will not be applied when testing in Expo client.

## Linking to your app

### In the Expo client

Before continuing it's worth taking a moment to learn how to link to your app within the Expo client. The Expo client uses the `exp://` scheme, but if we link to `exp://` without any address afterwards, it will open the app to the main screen.

In development, your app will live at a url like `exp://wg-qka.community.app.exp.direct:80`. When it's deployed, it will be at a URL like `exp://exp.host/@community/with-webbrowser-redirect`. If you create a website with a link like `<a href="exp://expo.io/@community/with-webbrowser-redirect">Open my project</a>`, then open that site on your device and click the link, it will open your app within the Expo client. You can link to it from another app by using `Linking.openURL` too.

### In a standalone app

To link to your standalone app, you need to specify a scheme for your app. You can register for a scheme in your `app.json` by adding a string under the `scheme` key (use only lower case):

```
{
  "expo": {
    "scheme": "myapp"
  }
}
```

Once you build your standalone app and install it to your device, you will be able to open it with links to `myapp://`.

If your app is ejected, note that like some other parts of `app.json`, changing the `scheme` key after your app is already ejected will not have the desired effect. If you'd like to change the deep link scheme in your ejected app, see [this guide](../../expokit/advanced-expokit-topics/#changing-the-deep-link-scheme).

### `Expo.Linking` module

To save you the trouble of inserting a bunch of conditionals based on the environment that you're in and hardcoding urls, we provide some helper methods in our extension of the `Linking` module. When you want to provide a service with a url that it needs to redirect back into your app, you can call `Expo.Linking.makeUrl()` and it will resolve to the following:

- _Published app in Expo client_: `exp://exp.host/@community/with-webbrowser-redirect`
- _Published app in standalone_: `myapp://`
- _Development_: `exp://wg-qka.community.app.exp.direct:80`

You can also change the returned url by passing optional parameters into `Expo.Linking.makeUrl()`. These will be used by your app to receive data, which we will talk about in the next section.

### Handling links into your app

There are two ways to handle URLs that open your app.

#### 1. If the app is already open, the app is foregrounded and a Linking event is fired

You can handle these events with `Linking.addEventListener('url', callback)`.

#### 2. If the app is not already open, it is opened and the url is passed in as the initialURL

You can handle these events with `Linking.getInitialURL` -- it returns a `Promise` that resolves to the url, if there is one.

See the examples below to see these in action.

### Passing data to your app through the URL

To pass some data into your app, you can append it as a path or query string on your url. `Expo.Linking.makeUrl(path, queryParams)` will construct a working url automatically for you. You can use it like this:

```javascript
let redirectUrl = Expo.Linking.makeUrl('path/into/app', { hello: 'world', goodbye: 'now' });
```

This would return something like `myapp://path/into/app?hello=world&goodbye=now` for a standalone app.

When your app is opened using the deep link, you can parse the link with `Expo.Linking.parse()` to get back the path and query parameters you passed in.

When [handling the URL that is used to open/foreground your app](#handling-urls-in-your-app), it would look something like this:

```javascript
_handleUrl = url => {
  this.setState({ url });
  let { path, queryParams } = Expo.Linking.parse(url);
  alert(`Linked to app with path: ${path} and data: ${JSON.stringify(queryParams)}`);
};
```

If you opened a URL like
`myapp://path/into/app?hello=world&goodbye=now`, this would alert
`Linked to app with path: path/into/app and data: {hello: 'world', goodbye: 'now'}`.

### Example: linking back to your app from WebBrowser

The example project [examples/with-webbrowser-redirect](https://github.com/expo/examples/tree/master/with-webbrowser-redirect) demonstrates handling redirects from `WebBrowser` and taking data out of the query string. [Try it out in Expo](https://expo.io/@community/with-webbrowser-redirect).

### Example: using linking for authentication

A common use case for linking to your app is to redirect back to your app after opening a [WebBrowser](../../sdk/webbrowser/). For example, you can open a web browser session to your sign in screen and when the user has successfully signed in, you can have your website redirect back to your app by using the scheme and appending the authentication token and other data to the URL.

**Note**: if try to use `Linking.openURL` to open the web browser for authentication then your app may be rejected by Apple on the grounds of a bad or confusing user experience. `WebBrowser.openBrowserAsync` opens the browser window in a modal, which looks and feels good and is Apple approved.

To see a full example of using `WebBrowser` for authentication with Facebook, see [examples/with-facebook-auth](https://github.com/expo/examples/tree/master/with-facebook-auth). Currently Facebook authentication requires that you deploy a small webserver to redirect back to your app (as described in the example) because Facebook does not let you redirect to custom schemes, Expo is working on a solution to make this easier for you. [Try it out in Expo](https://expo.io/@community/with-facebook-auth).

Another example of using `WebBrowser` for authentication can be found at [expo/auth0-example](https://github.com/expo/auth0-example).

## Universal/deep links (without a custom scheme)

It is often desirable for regular HTTPS links (without a custom URL scheme) to directly open your app on mobile devices. This allows you to send notification emails with links that work as expected in a web browser on desktop, while opening the content in your app on mobile. iOS refers to this concept as "universal links" while Android calls it "deep links" (but in this section, we are specifically discussing deep links that do not use a custom URL scheme).

### Universal links on iOS

To implement universal links on iOS, you must first set up verification that you own your domain. This is done by serving an Apple App Site Association (AASA) file from your webserver. The AASA must be served from `/.well-known/apple-app-site-association` (with no extension). The AASA contains JSON which specifies your Apple app ID and a list of paths on your domain that should be handled by your mobile app. For example, if you want links of the format `https://www.myapp.io/records/123` to be opened by your mobile app, your AASA would have the following contents:

```
{
  "applinks": {
    "apps": [],
    "details": [{
      "appID": "LKWJEF.io.myapp.example",
      "paths": ["/records/*"]
    }]
  }
}
```

This tells iOS that any links to `https://www.myapp.io/records/*` (with wildcard matching for the record ID) should be opened directly by your mobile app. See [Apple's documentation](https://developer.apple.com/documentation/uikit/core_app/allowing_apps_and_websites_to_link_to_your_content/enabling_universal_links) for further details on the format of the AASA. Branch provides an [AASA validator](https://branch.io/resources/aasa-validator/) which can help you confirm that your AASA is correctly deployed and has a valid format.

Note that iOS will download your AASA when your app is first installed and when updates are installed from the App Store, but it will not refresh any more frequently. If you wish to change the paths in your AASA for a production app, you will need to issue a full update via the App Store so that all of your users' apps re-fetch your AASA and recognize the new paths.

After deploying your AASA, you must also configure your app to use your associated domain. First, you need to add the `associatedDomains` [configuration](../../workflow/configuration#ios) to your `app.json`. Second, you need to edit your App ID on the Apple developer portal and enable the "Associated Domains" application service. To do so go in the App IDs section and click on your App ID. Select Edit, check the Associated Domains checkbox and click Done. You will also need to regenerate your provisioning profile after adding the service to the App ID.

At this point, opening a link on your mobile device should now open your app! If it doesn't, re-check the previous steps to ensure that your AASA is valid, the path is specified in the AASA, and you have correctly configured your App ID in the Apple developer portal. Once you've got your app opening, move to the [Handling links into your app](#handling-links-into-your-app) section for details on how to handle the inbound link and show the user the content they requested.

### Deep links on Android

Implementing deep links on Android (without a custom URL scheme) is somewhat simpler than on iOS. You simply need to add `intentFilters` to the [Android section](../../workflow/configuration#android) of your `app.json`. The following basic configuration will cause your app to be presented in the standard Android dialog as an option for handling any record links to `myapp.io`:

```
"intentFilters": [
  {
    "action": "VIEW",
    "data": [
      {
        "scheme": "https",
        "host": "*.myapp.io",
        "pathPrefix": "/records"
      },
    ],
    "category": [
      "BROWSABLE",
      "DEFAULT"
    ]
  }
]
```

It may be desirable for links to your domain to always open your app (without presenting the user a dialog where they can choose the browser or a different handler). You can implement this with Android App Links, which use a similar verification process as Universal Links on iOS. First, you must publish a JSON file at `/.well-known/assetlinks.json` specifying your app ID and which links should be opened by your app. See [Android's documentation](https://developer.android.com/training/app-links/verify-site-associations) for details about formatting this file. Second, add `"autoVerify": true` to the intent filter in your `app.json`; this tells Android to check for your `assetlinks.json` on your server and register your app as the automatic handler for the specified paths:

```
"intentFilters": [
  {
    "action": "VIEW",
    "autoVerify": true,
    "data": [
      {
        "scheme": "https",
        "host": "*.myapp.io",
        "pathPrefix": "/records"
      },
    ],
    "category": [
      "BROWSABLE",
      "DEFAULT"
    ]
  }
]
```

## When to _not_ use deep links

This is the easiest way to set up deep links into your app because it requires a minimal amount of configuration.

The main problem is that if the user does not have your app installed and follows a link to your app with its custom scheme, their operating system will indicate that the page couldn't be opened but not give much more information. This is not a great experience. There is no way to work around this in the browser.

Additionally, many messaging apps do not autolink URLs with custom schemes -- for example, `exp://exp.host/@community/native-component-list` might just show up as plain text in your browser rather than as a link ([exp://exp.host/@community/native-component-list](exp://exp.host/@community/native-component-list)).

An example of this is Gmail which strips the href property from links of most apps, a trick to use is to link to a regular https url instead of your app's custom scheme, this will open the user's web browser. Browsers do not usually strip the href property so you can host a file online that redirects the user to your app's custom schemes.

So instead of linking to example://path/into/app, you could link to https://example.com/redirect-to-app.html and redirect-to-app.html would contain the following code:

```javascript
<script>window.location.replace("example://path/into/app");</script>
```
