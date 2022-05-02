# Bond-Buddy

## About
**Bond Buddy is an enterprise facing Jetpack Compose app built for bonding agencies. The app's core functionality is to provide client location data to bondsmans.
Secondary functionalities include storing contact details and notifiying company owners of various client actions.
Bond Buddy is designed to be used with high risk clients to provide companies with peace of mind and protect their assests.**

## Cont..
Bond buddy starts onboarding by having the user register as either a client or bondsman, followed by the naming or joining of the company.
Bondsman are able to see a list of users as well as their "active" status, which is determined by the timestamp of the last recorded location.
Bondsman have an interactive map that contains user location data organized into a list of days, with a nested list of locations in each day. Bondsman can view each location blip on the map and tap it for more details including coordinates and a timestamp. Bondsman can manage users of their company, deleting or removing as needed. Bond Buddy also always the bondsman to bind clients to selected states and recieve notifications if they leave them.

## Technologies and Libraries Used
1. Google Cloud Services (FCM, Firebase, Auth)
2. Dagger/Hilt Dependency Injection
3. Kotlin Coroutines
4. Jetpack Compose
5. Work Manager
6. Google Maps API
7. CameraX

##	Notable Problems
During the developement of Bond Buddy the Android 12 update added restrictions to background work and heavily restricted background location access. I also had some issues with doze mode affecting the reliability of the hourly locations updates. This required a few steps to work around. First I simply had to add new permission requests for accessing location in the background AFTER being granted fine and coarse location access. The next issue was a bit more tricky. Due to doze mode and background location access restrictions I had to figure out a way to remotely wake up the phone and run my functions. To do this I used Cloud Messaging to send hourly data messages to all clients. This data message will be recieved by a service on the phone and pull it out of doze, with limited network access, temporarily by creating a notification from the data message. This surficed the doze and background restriction problems. The only caveat was the OS would stop showing notifications if they repeatedly failed to lead to user interactoin with the app. The workaround was to prompt the user to interact with the app to refresh their active status. Bondsman are notified when users become inactive.

## Screenshots
| Bondsman | Client | Map |
|:---------------------------------:|:---------------------------------:|:---------------------------------:|
| <img src="https://user-images.githubusercontent.com/49169067/166229208-14059ddb-1c3b-4257-824a-c76b3332f9b8.jpg" height="500" width ="250"> | <img src="https://user-images.githubusercontent.com/49169067/166229346-c3fd4b42-9f9a-4725-960c-8929fa874ef6.jpg" height="500" width ="250"> | <img src="https://user-images.githubusercontent.com/49169067/166231503-9346013e-7be3-4d8b-93ed-270dd874a50b.jpg" height="500" width ="250"> |

### Onboarding
<img src="https://user-images.githubusercontent.com/49169067/166229172-101d0265-867d-428d-a080-f8e03530692e.jpg" height="500" width ="250"> 
<img src="https://user-images.githubusercontent.com/49169067/166232272-5f031169-f18d-4e76-aed8-e3ce1c39175d.jpg" height="500" width ="250"> 
<img src="https://user-images.githubusercontent.com/49169067/166231977-c186ff9d-2591-4819-b828-9a93c88ddf48.jpg" height="500" width ="250">
<img src="https://user-images.githubusercontent.com/49169067/166232358-8a0a7c41-5164-45da-ae6f-91db1e580f1c.jpg" height="500" width ="250">
<img src="https://user-images.githubusercontent.com/49169067/166232426-fb1e3138-a342-4ea9-bd7e-b2f941962b24.jpg" height="500" width ="250">
