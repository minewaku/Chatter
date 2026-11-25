// model
- remove ImageDimensions since profile doesnt need to know about them, also removing out of the model package
- remove ImageRef since it belongs to old designs
- avatar must be square so ImageDimensions may still be useful but modifying it in a smart way and put it else where 
- OwnedImage has nothing to do with bussiness domain so may it belong to services/profile/profile-core/src/main/java/com/minewaku/chatter/profile/profile/internal/application/command

then do not test anything



