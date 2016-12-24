# GIFView
This is a library for showing GIFs in applications.

## XML Attributes:
 
 starting_on_init:
 A boolean that represents if the view starts the gif
 when its initialization finishes or not.
 
 
 delay_in_millis:
 A positive integer that represents how many milliseconds
 should pass between every calculation of the next frame to be set.
 
 gif_src:
 A string that represents the gif's source.
 
 - If you want to get the gif from a url
 concatenate the string "url:" with the full url.
 
 - if you want to get the gif from the assets directory
 concatenate the string "asset:" with the full path of the gif
 within the assets directory. You can exclude the .gif extension.
 
 for example if you have a gif in the path "assets/ex_dir/ex_gif.gif"
 the string should be: "asset:ex_dir/ex_gif"

## Code Example

From XML:
```xml
<com.whygraphics.gifview.gif.GIFView xmlns:gif_view="http://schemas.android.com/apk/res-auto"
        android:id="@+id/main_activity_gif_vie"
        android:layout_width="200dp"
        android:layout_height="200dp"
        android:scaleType="center"
        gif_view:gif_src="url:http://pop.h-cdn.co/assets/16/33/480x264/gallery-1471381857-gif-season-2.gif" />
```

In the activity:
```java
GIFView mGifView = (GIFView) findViewById(R.id.main_activity_gif_vie);
        
mGifView.setOnSettingGifListener(new GIFView.OnSettingGifListener() {
            @Override
            public void onSuccess(GIFView view) {
                Toast.makeText(MainActivity.this, "onSuccess()", Toast.LENGTH_SHORT).show();
            }

            @Override
            public void onFailure(GIFView view, Exception e) {

            }
});
```

Setting the gif programmatically:
```java
mGifView.setGifResource("asset:gif1");
```
