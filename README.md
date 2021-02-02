# ProPicker

A simple library to select images from the gallery and camera with RTL support.


##### Functions

## For Camera

1. `cameraOnly()` -> To open the CameraX only
3. `crop()` -> Only works with camera
3. `compressImage` -> compresing image work for both gallery and camera


## Gallery related function
4. `galleryOnly()` -> To open the gallery view only
5. `singleSelection` -> Pick single file
6. `multiSelection` -> Pick multi file and get the result as ArrayList    
7. `maxResultSize` -> Max Width and Height of final image
8. `compressImage` -> compresing image work for both gallery and camera
9. `compressVideo` -> (Under Development)
10. `onlyImage` -> Select image from gallery
11. `onlyVideo` -> Select video from gallery

## Receiver the result

12. `ProPicker.getPickerDataAsByteArray(this, intent)` -> Returns all the data as ByteArray 
13. `ProPicker.getSelectedPickerDatas(intent: Intent)` -> Get all the data 
14. `ProPicker.getPickerData(intent: Intent)` -> Get single data 


### Appreciation

Thanks to shaon2016 for his awesome work. I'll keep updating and adding more features to it in future.
