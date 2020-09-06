package com.weiliu.library.browser;

/*
 * Copyright (C) 2008 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import android.app.Activity;
import android.content.ContentValues;
import android.os.AsyncTask;
import android.support.annotation.Nullable;

/**
 * This class is used to pull down the http headers of a given URL so that
 * we can analyse the mimetype and make any correction needed before we give
 * the URL to the download manager. The ContentValues class holds the
 * content that would be provided to the download manager, so that on
 * completion of checking the mimetype, we can issue the download to
 * the download manager.
 * This operation is needed when the user long-clicks on a link or image and
 * we don't know the mimetype. If the user just clicks on the link, we will
 * do the same steps of correcting the mimetype down in
 * android.os.webkit.LoadListener rather than handling it here.
 *
 */
class FetchUrlMimeType extends AsyncTask<ContentValues, String, String> {

    /**Activity instance.*/
    Activity mActivity;
    
    /**Download content values.*/
    ContentValues mValues;

    /**
     * Constructor.
     * @param activity activity
     * */
    public FetchUrlMimeType(Activity activity) {
        mActivity = activity;
    }

    @Nullable
    @Override
    public String doInBackground(ContentValues... values) {
//        mValues = values[0];
//
//        // Check to make sure we have a URI to download
//        String uri = mValues.getAsString(Downloads.Impl.COLUMN_URI);
//        if (uri == null || uri.length() == 0) {
//            return null;
//        }
//
//        HttpHead request = null;
//        try {
//        	request = new HttpHead(uri);
//        } catch (IllegalArgumentException ex) {
//        	ex.printStackTrace();
//        	return null;
//        }
//
//        // User agent is likely to be null, though the AndroidHttpClient
//        // seems ok with that.
//        HttpClient client = new DefaultHttpClient();
//
//
//        String cookie = mValues.getAsString(Downloads.Impl.COLUMN_COOKIE_DATA);
//        if (cookie != null && cookie.length() > 0) {
//            request.addHeader("Cookie", cookie);
//        }
//
//        String referer = mValues.getAsString(Downloads.Impl.COLUMN_REFERER);
//        if (referer != null && referer.length() > 0) {
//            request.addHeader("Referer", referer);
//        }
//
//        HttpResponse response;
//        String mimeType = null;
//        try {
//            response = client.execute(request);
//            // We could get a redirect here, but if we do lets let
//            // the download manager take care of it, and thus trust that
//            // the server sends the right mimetype
//            if (response.getStatusLine().getStatusCode() == HttpStatus.SC_OK) {
//                Header header = response.getFirstHeader("Content-Type");
//                if (header != null) {
//                    mimeType = header.getValue();
//                    final int semicolonIndex = mimeType.indexOf(';');
//                    if (semicolonIndex != -1) {
//                        mimeType = mimeType.substring(0, semicolonIndex);
//                    }
//                }
//            }
//        } catch (IllegalArgumentException ex) {
//            request.abort();
//        } catch (IOException ex) {
//            request.abort();
//        } catch (Exception ex) {
//        	request.abort();
//        }
//
//        return mimeType;
        return null;
    }

   @Override
    public void onPostExecute(@Nullable String mimeType) {
//       if (mimeType != null) {
//           String url = mValues.getAsString(Downloads.Impl.COLUMN_URI);
//           if (mimeType.equalsIgnoreCase("text/plain")
//                   || mimeType.equalsIgnoreCase("application/octet-stream")) {
//               String newMimeType =
//                       MimeTypeMap.getSingleton().getMimeTypeFromExtension(
//                           MimeTypeMap.getFileExtensionFromUrl(url));
//               if (newMimeType != null) {
//                   mValues.put(Downloads.Impl.COLUMN_MIME_TYPE, newMimeType);
//               }
//           }
//           String filename = URLUtil.guessFileName(url,
//                   null, mimeType);
//           mValues.put(Downloads.Impl.COLUMN_FILE_NAME_HINT, filename);
//       }
//
//       // Start the download
//      /* final Uri contentUri =
//           */mActivity.getContentResolver().insert(Downloads.Impl.CONTENT_URI, mValues);
    }

}
