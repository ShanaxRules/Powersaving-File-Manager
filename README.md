#Built a simple file manager for android that is lightweight and has a simple colored UI , and can be used for managing , open and protecting files by using password encryption . 

The file manager application operates on the following system model:
• User Interface:
– A user-friendly interface is provided for browsing files and folders.
– Essential actions like opening, copying, deleting, and renaming files
are accessible through intuitive buttons and menus.
• File System Interaction:
– The application interacts with the device’s file system to access and
manipulate files and folders.
– It uses appropriate APIs to read, write, and delete files.
• Password Protection:
– A password protection mechanism (SHA-256 algorithm) is implemented to safeguard sensitive files.
– The user is prompted to enter a password on application launch.
– The entered password is hashed and compared with the stored hash
for verification.
• Minimizing Power Consumption:
– UI designed with simple color gradients with no icons so that no
processes are used for loading any images.
– Especially programmed to make the application work as a standalone
lite handler instead of creating it’s own process to manage the files
so that the OS of the android does most of the work instead of the
app itself

Tools used : Java , Kotlin , Android Studio and Basic Java hashing SHA256

