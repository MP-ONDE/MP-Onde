2024.11.14
Connect firebase auth, firestore.
Build 3 activities Main, Login(signup), profileEdit.
Add App class for wrapper activities.
Add Hashutils for genereating unifrom userIdHash
Edit gradles and Manifest
Make repository menu for menu, topbar

Main
- Specify requiredPermissions, Bluetooth and location
- Implement permission requests
- Implement bluetooth advertising and scanning(max 12 sec)
- Build callbacks for advertising and scanning
- Made simple views with icons by vector asset
Login(signup)
- Only email sign up available
- Password length >=6
- Can use main activity only after login
- user id, userIdHash, username, profile is stored after signup until 2024.12.13
ProfileEdit
- Implement editing username and profile

# MP-Onde