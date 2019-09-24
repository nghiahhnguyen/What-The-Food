What The Food 101
==============================

### Idea: 
Scan for food in image in real-time => User can read info about the food => User can book the food through Foody.vn website.

### Main features:
- Firebase ML Kit Object Detection Integration
- Training a Food Classification model with Tensorflow
- Convert the model to TFLite
- Integrate model to classify output by Firebase ML Kit as food categories
- Handling Asynchronous nature of Inference by the model
- Carousel view with image
- Network handling and Http request with Volley library
- Get API from Shutterstock 
- Get image by Picasso library
- Combine 6, 7, 8, 9
- food_info database for 101 types of food
- Firestore from Firebase  

### Drawback:
- The model is not extremely accurate. Therefore, it can predict the wrong type of food.

### Future works:
- Improving the model for food recognition and classification. I am thinking about trying a YOLO model, which should improve the accuracy from our current model - Conv Net.

### Demo:
![Demo of the app](https://github.com/huunghia160799/What-The-Food/blob/master/what-the-food-demo-.gif)

License
-------


Licensed to the Apache Software Foundation (ASF) under one or more contributor
license agreements.  See the NOTICE file distributed with this work for
additional information regarding copyright ownership.  The ASF licenses this
file to you under the Apache License, Version 2.0 (the "License"); you may not
use this file except in compliance with the License.  You may obtain a copy of
the License at

  http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
License for the specific language governing permissions and limitations under
the License.
