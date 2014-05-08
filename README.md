antrace
=======

This app is a port and front end of potrace(<http://potrace.sourceforge.net/>) on Android! It can transform a bitmap into a vector graphic.

###How to use this app

To use this app, firstly please select a photo on your phone or take a photo. Next you can crop the photo, select the region you want to trace:

![Image](https://raw.githubusercontent.com/jiangpeng79/antrace/master/snapshots/crop.png)

Next step please specify a threshold value for black/white image, potrace will process the output black/white image:

![Image](https://raw.githubusercontent.com/jiangpeng79/antrace/master/snapshots/threshold.png)

Now you will see a preview of output vector graphic:

![Image](https://raw.githubusercontent.com/jiangpeng79/antrace/master/snapshots/preview.png)

Save it to file, we support svg and dxf formats:

![Image](https://raw.githubusercontent.com/jiangpeng79/antrace/master/snapshots/save.png)

###How to build this app

To build this app, firstly please setup Android development environment(I am using ADT Bundle). Then please import the project to your workspace. Potrace is a c language program, so you will also need NDK to build.

Thanks
