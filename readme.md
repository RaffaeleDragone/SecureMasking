# Secure Video Masking

In recent decades, it has been observed how technological evolution has led us to share images and videos relating to our private life. 

Sharing them on social networks or exchanged via instant messaging without worrying about our privacy, not knowing unconsciously that that photo / video shared, can spread sensitive information relating to the people depicted. 

These sites usually offer their users to have some control over their privacy protection. For example, it allows the user to decide who to show their photo / video, thus reducing only to the people with whom they want to share it, that is, a simple conditional access. 

Disagreeing with people who don't know each other to access it. However, even today researchers in the field of image processing and media security propose various approaches to allow the privacy of photos, which techniques focus on the encryption or permutation of all image data. From a data security perspective, an encryption-based scheme can securely protect privacy and ensure reversibility. Yet, encrypting only an entire image can significantly affect the ease of sharing the photo. It is obvious that people are always looking for simple and intuitive solutions to share their photos online to the public, protecting some specific areas of an image, blurring or making some sensitive areas unreadable, even by means of smileys and stickers. Nevertheless, all the manipulations described are not reversible. The goal is to explore new solutions to protect the privacy of an image in a safe and reversible way. Thus allowing the original image to be reconstructed with almost lossless quality, even if the encrypted image had been manipulated.

The project in its first version consisted of applying Masking and Unmasking techniques to the sensitive biometries contained within jpeg images, the sensitive data useful for the reconstruction of the content was inserted within the metadata of the image itself. This first version is subsequently resumed and expanded by inserting an information hiding process using Steganography techniques, so the previous storage of data within the metadata leaves room for storage within the image pixels using the OpenStego opensource software. 

Our work aims to expand this system or to provide the possibility of applying Masking and Unmasking techniques on video while integrating a secure storage of the replaced data through an ad hoc Steganography technique.

