package com.klinker.android.twitter_l.ui.tweet_viewer;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.*;
import com.klinker.android.twitter_l.R;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.HttpClient;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.DefaultHttpClient;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;

public class VideoFragment extends Fragment {

    public Context context;
    public String tweetUrl;
    public String videoUrl;

    public VideoView video;

    public VideoFragment() {

    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        context = activity;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        tweetUrl = getArguments().getString("url");

        View layout = inflater.inflate(R.layout.gif_player, null, false);
        video = (VideoView) layout.findViewById(R.id.gif);

        if (tweetUrl.contains("video.twimg")) {
            MediaController mediaController = new MediaController(getActivity());
            mediaController.setAnchorView(video);
            video.setMediaController(mediaController);
        }

        getGif();

        return layout;
    }

    public void getGif() {
        new Thread(new Runnable() {
            @Override
            public void run() {

                if (tweetUrl.contains("vine.co")) {
                    // have to get the html from the page and parse the video from there.

                    videoUrl = getVineLink();
                } else if (tweetUrl.contains("/photo/1") && tweetUrl.contains("twitter.com/")) {
                    // this is before it was added to the api.
                    // finds the video from the HTML on twitters website.

                    videoUrl = getGifLink();
                } else {
                    videoUrl = tweetUrl;
                }

                getActivity().runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            if (videoUrl != null) {
                                final Uri videoUri = Uri.parse(videoUrl);

                                video.setVideoURI(videoUri);
                                video.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                                    @Override
                                    public void onPrepared(MediaPlayer mp) {

                                        video.setBackgroundColor(getActivity().getResources().getColor(android.R.color.transparent));
                                        mp.setLooping(true);
                                    }
                                });

                                video.start();
                            } else {
                                Toast.makeText(getActivity(), R.string.error_gif, Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            // not attached to activity
                        }
                    }
                });

            }
        }).start();
    }

    public Document getDoc() {
        try {
            HttpClient httpclient = new DefaultHttpClient();
            HttpGet httpget = new HttpGet((tweetUrl.contains("http") ? "" : "https://") + tweetUrl);
            HttpResponse response = httpclient.execute(httpget);
            HttpEntity entity = response.getEntity();
            InputStream is = entity.getContent();
            BufferedReader reader = new BufferedReader(new InputStreamReader(is, "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null)
                sb.append(line + "\n");

            String docHtml = sb.toString();

            is.close();

            return Jsoup.parse(docHtml);
        } catch (Exception e) {
            return null;
        }
    }

    public String getGifLink() {
        try {
            Document doc = getDoc();

            if(doc != null) {
                Elements elements = doc.getElementsByAttributeValue("class", "animated-gif");

                for (Element e : elements) {
                    for (Element x : e.getAllElements()) {
                        if (x.nodeName().contains("source")) {
                            return x.attr("video-src");
                        }
                    }
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        return null;
    }

    public String getVineLink() {
        try {
            Document doc = getDoc();

            if(doc != null) {
                Elements elements = doc.getElementsByAttributeValue("property", "twitter:player:stream");

                for (Element e : elements) {
                    return e.attr("content");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        } catch (OutOfMemoryError e) {
            e.printStackTrace();
        }

        return null;
    }
}
