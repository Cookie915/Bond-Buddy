/* eslint-disable max-len */
/* eslint-disable prefer-const */
/* eslint-disable no-trailing-spaces */
const functions = require("firebase-functions");
const admin = require("firebase-admin");
const {firestore} = require("firebase-admin");
const PromisePool = require("es6-promise-pool").default;
admin.initializeApp();

exports.locationUpdates = functions.pubsub.schedule("every 1 hours")
    .onRun( async (context)=>{
      let payload = {
        "data": {
          "title": "Location Updated",
          "body": "Tap this notification to ensure active status!",
          "category": "locationupdate",
        },
      };
      const options = {
        "android": {
          "priority": "high",
          "ttl": "0s",
        },
      };
      admin.messaging().sendToTopic("LocationUpdates", payload, options)
          .catch((error) => {
            console.error(error);
          });
    });

exports.directLocationUpdate = functions.https
    .onCall(async (data)=>{
      const id = data.id;
      console.log(id);
      const companyName = data.companyName;
      console.log(companyName);
      let tokenArray = await firestore().collection("fcmtokens")
          .where("id", "==", id).limit(1).get();
      const tokenData = tokenArray.docs.pop();
      let token = tokenData.get("token");
      console.log(token);
      let payload = {
        "data": {
          "title": "Location Update",
          "body": "Location Update Requested",
          "category": "requestedlocation",
        },
      };
      const options = {
        "android": {
          "priority": "High",
          "ttl": "0s",
          "collapseKey": "LocationUpdate",
        },
      };
      admin.messaging().sendToDevice(token, payload, options)
          .then((response) => {
            console.log("Message Sent To Server");
          }).catch((error) => {
            console.log("Error Sending Message:", error);
          });
    });

// data contains sender args,
// context contains auth info and stuff sent automatically from app
exports.notifyFailedLocationUpdate = functions.https
    .onCall((data, context)=>{
      const companyName = data.companyname;
      const name = data.name;
      let payload = {
        "notification": {
          "title": "Location Update Failed",
          "body": "Couldn't get Location Update For " + name,
        },
        "data": {
          "category": "failedlocationupdate",
        },
        "android": {
          "priority": "normal",
        },
      };
      admin.messaging().sentToTopic(companyName, payload);
    });

// send notification when user leaves bounding state
exports.notifyUserLeftState = functions.https
    .onCall((data)=>{
      const companyName = data.companyname;
      const companyNameString = companyName.split(" ").join("");
      const name = data.name;
      functions.logger.debug(companyName);
      let payload = {
        "data": {
          "title": "User Left Bounded State",
          "body": "User "+ name + " Was recorded Outside Bounded State " +
       firestore.Timestamp.now().toDate().toLocaleDateString(),
          "category": "boundscheck",
          "username": name,
        },
      };
      admin.messaging().sendToTopic(companyNameString, payload);
    });

exports.notifyUserDeletedAccount = functions.https
    .onCall((data)=>{
      const companyName = data.companyname;
      const companyNameString = companyName.split(" ").join("");
      const name = data.name;
      const timestamp = firestore.Timestamp.now()
          .toDate().toLocaleDateString();
      let message = {
        "notification": {
          "title": "User Deleted Account",
          "body": "User "+ name + " Deleted Account On " +
        timestamp + ", Data Cached",
        },
        "data": {
          "category": "uninstall",
        },
      };
      admin.messaging().sendToTopic(companyNameString, message);
    });

exports.markUsersInactive = functions.pubsub.schedule("every 1 hours")
    .onRun(async (context)=>{
      const secondsInDay = 86400;
      let inactiveThreshhold = firestore.Timestamp.now().seconds - secondsInDay;
      const users = firestore().collection("users").get();
      (await users).docs.forEach((doc)=>{
        let lastlocation = doc.get("lastlocation");
        let name = doc.get("displayname");
        let active = doc.get("active");
        let timestamp = lastlocation.timestamp._seconds;
        if (timestamp < inactiveThreshhold) {
          if (active == true && doc.get("owner") == false) {
            console.log(name);
            console.log("Mark Inactive");
            let data = {
              "active": false,
            };
            doc.ref.set(data, {merge: true});
            let companyName = doc.get("companyname");
            let companyString = companyName.split(" ").join("");
            console.log(companyString);
            let message = {
              "data": {
                "title": "User Went Inactive",
                // eslint-disable-next-line max-len
                "body": "User "+ name + " become inactive on " + firestore.Timestamp.now().toDate().toLocaleDateString(),
                "category": "markinactive",
                "username": name,
              },
            };
            admin.messaging().sendToTopic(companyString, message);
          }
        } else
        if (timestamp > inactiveThreshhold) {
          console.log(name);
          console.log("Mark Active");
          let data = {
            "active": true,
          };
          doc.ref.set(data, {merge: true});
        }
      });
    });

exports.cleanUpOldTokens = functions.pubsub.schedule("0 0 1 * *")
    .onRun(async (context)=>{
      const twoMonthsInMillis = 5259600000;
      const currentTimestamp = admin.firestore.Timestamp.now();
      const minTimestamp = admin.firestore.Timestamp.fromMillis(currentTimestamp.toMillis()-twoMonthsInMillis);
      const fcmtokens = await admin.firestore().collection("fcmtokens").get();
      //  array of Promises of Tokens to Remove
      let tokensToRemove = [];
      //  ids of users to remove
      let usersToCache = [];
      //  tokens listed in an Array
      const inactiveTokens = fcmtokens.docs.filter((doc) =>{
        if (doc.get("timestamp") < minTimestamp) {
          return true;
        } else {
          return false;
        }
      });
      if (inactiveTokens.length > 0) {
        inactiveTokens.forEach((token) => {
          tokensToRemove.push(token.ref.delete());
        });
        const userData = {
          "active": false,
          "cached": true,
        };
        inactiveTokens.forEach((Token)=>{
          usersToCache.push(admin.firestore().collection("users").doc(Token.get("id")).set(userData, {merge: true}));
        });
      }
      let payload = {
        "data": {
          "category": "cleanuptokens",
        },
      };
      let responses = admin.messaging().sendToDevice(fcmtokens, payload);
      (await responses).results.forEach((response, index) => {
        const error = response.error;
        if (error) {
          const userData = {
            "active": false,
            "cached": true,
          };
          //  Cleanup tokens that are no longer registered
          if (error.code === "messaging/invalid-registration-token" ||
                error.code === "messaging/registration-token-not-registered") {
            usersToCache.push(admin.firestore().collection("users").doc(fcmtokens.docs.at(index).get("id")).set(userData, {merge: true}));
            tokensToRemove.push(fcmtokens.docs.at(index).ref.delete());
          }
        }
        return Promise.all(tokensToRemove, usersToCache);
      });
    }); 

exports.cleanUpOldCompanies = functions.pubsub.schedule("0 0 1 */3 *")
    .onRun(async (context)=>{
      const threeMonthsInMillis = 7889300000;
      (await admin.firestore().collection("companies").get()).forEach((company)=>{
        let owner =company.get("owner");
        admin.auth().getUser(String(owner)).then(
            //  fulfilled
            (userRecord)=>{
              console.log("Got User Records");
              //  if company owner inactive too long delete
              if (userRecord.metadata.lastRefreshTime < (Date.now() - threeMonthsInMillis)) {
                //  Delete Company Members 
                cleanCompany(company.get("companyname"));
                //  Delete Company
                admin.firestore().recursiveDelete(company.ref);
                //  Delete Company Owner's Auth Data
                admin.auth().deleteUser(userRecord.uid);
                //  Clear Company Owner's Tokens
                admin.firestore().collection("fcmtokens").where("id", "==", userRecord.uid).get().then((matchingTokens)=>{
                  matchingTokens.forEach((token)=>{
                    token.ref.delete();
                  });
                });
                //  Delete Company Owner
                let ownerRef = admin.firestore().collection("users").doc(owner);
                admin.firestore().recursiveDelete(ownerRef);
              }
            },
            (error)=>{
            //  rejected, user no longer exists
              console.log("No User Records Present");
              //  Delete Company Members
              cleanCompany(company.get("companyname"));
              //  Delete Company
              admin.firestore().recursiveDelete(company.ref);
            },
        );
      });
    });

// eslint-disable-next-line require-jsdoc
async function cleanCompany(companyName) {
  console.log("cleaning company");
  (await admin.firestore().collection("companies").doc(companyName).collection("memberIDs").get()).forEach((memberID) => {
    console.log("Deleting member" + memberID.get("user").split("/")[1]);
    let memberRef = admin.firestore().doc(memberID.get("user"));
    let uid = String(memberID.get("user")).split("/")[1];
    //  Delete Tokens Matching User's UID
    admin.firestore().collection("fcmtokens").where("id", "==", uid).get().then((matchedTokens)=>{
      matchedTokens.forEach((token)=>{
        token.ref.delete();
      });
      //  Delete User and Nested Data
      admin.firestore().recursiveDelete(memberRef);
      //  Delete User's Auth Data
      admin.auth().deleteUser(uid);
    });
  });
}
      
//  Cleans up inactive users from fireb ase auth
exports.cleanUpOldAuthUsers = functions.pubsub.schedule("every day 00:00")
    .onRun(async (context)=>{
      // Maximum concurrent account deletions.
      const MAX_CONCURRENT = 3;
      const inactiveUsers = await getInactiveUsers();
      inactiveUsers.forEach((user)=>{
        let id = user.id;
        let data = {
          "cached": true,
          "active": false,
        };
        admin.firestore().collection("users").doc(id).set(data, {merge: true});
      });
      // Use a pool so that we delete maximum `MAX_CONCURRENT` users in parallel.
      // eslint-disable-next-line max-len
      const promisePool = new PromisePool(() => deleteInactiveUser(inactiveUsers), MAX_CONCURRENT);
      await promisePool.start();
      functions.logger.log("User auth cleanup finished");
    });

// Returns the list of all inactive users.
// eslint-disable-next-line require-jsdoc
async function getInactiveUsers(users = [], nextPageToken) {
  const result = await admin.auth().listUsers(1000, nextPageToken);
  // Find users that have not signed in in the last 30 days.
  const inactiveUsers = result.users.filter(
      (user) => Date.parse(user.metadata.lastRefreshTime ||
            // eslint-disable-next-line max-len
            user.metadata.lastSignInTime) < (Date.now() - 30 * 24 * 60 * 60 * 1000));

  // eslint-disable-next-line max-len
  // Concat with list of previously found inactive users if there was more than 1000 users.
  users = users.concat(inactiveUsers);

  // If there are more users to fetch we fetch them.
  if (result.pageToken) {
    return getInactiveUsers(users, result.pageToken);
  }
  return users;
}

// eslint-disable-next-line require-jsdoc
async function deleteInactiveUser(inactiveUsers) {
  if (inactiveUsers.length > 0) {
    const userToDelete = inactiveUsers.pop();

    // Delete the inactive user.
    try {
      await admin.auth().deleteUser(userToDelete.uid);
      return functions.logger.log(
          "Deleted user account",
          userToDelete.uid,
          "because of inactivity");
    } catch (error) {
      return functions.logger.error(
          "Deletion of inactive user account",
          userToDelete.uid,
          "failed:",
          error);
    }
  } else {
    return null;
  }
}

