# NextBill Server

**The easy way to split and manage invoices**

![](https://raw.githubusercontent.com/nextbill-project/server/master/docs/webapp_screenshot.png)

## NextBill is great for... ##

- **Secure data storage** Store your invoices, receipt images and expense reports securely on your server, NAS or desktop computer
- **Anytime Data Access** You can access your data anytime and from anywhere via Android (even offline) and web app
- **Intelligent input assistants** Automatic completion as well as e-mail reception, image upload and voice input support you during data input
- **Split Invoices** Split bills and create expense reports for your partner, roommate or friend
- **Billing Workflow** Always keep track of open invoices and the workflow of cost settlements
- **Define cost limits** Create budgets and be automatically warned when they are exceeded
- **Data analysis** Evaluate your data using a series of charts and key values or look at forecasts
- **User administration** Manage users and give them different rights, depending on your needs

## Installation
The Java architecture allows NextBill to be installed on almost any system. 
Regarding access, two scenarios can be distinguished:

1. You install NextBill on your NAS or desktop computer (Windows, Mac or Linux) at home. In this case you have access only in the local network. NextBill can then be reached via IP in any browser and in the Android app. As soon as you leave home, the Android app switches to offline mode, but synchronizes when you return.

2. You install NextBill on a (cloud) server. In this case you can access it from anywhere via browser or the Android app. But keep in mind that such a configuration tends to have higher security risks.

## Quick-Setup
1. Install version 1.8 of Java Runtime Environment [Download](https://www.oracle.com/java/technologies/javase-jre8-downloads.html)
2. Download the 'NextBill.jar' and copy it to a folder of your choice.
3. **On Windows or Mac** double click on the Jar
**On Linux or without display screen** open a terminal and enter:
```
cd 'path/to/your/jar'
java -jar nextbill.jar
```
4. Open a browser with the URL [http://localhost:8010](http://localhost:8010)

You should run NextBill on every system startup for easy access. You will soon find more details in the Wiki.

##Contribution

You want to contribute? That's great! Please use the following steps and your changes will be visible very soon!

### How to contribute

1. Tell us your objective by writing or commenting an issue
2. Create a branch and implement the issue of your choice
3. Commit with a meaningful message and the issue ID
4. Create a pull request and wait for feedback
5. Drink a beer and be proud of your work :)

All contributions to this repository are considered to be licensed under the AGPLv3 or any later version.

### Tools for development

- [Java Delopment Kit (JDK) 1.8](https://www.oracle.com/java/technologies/javase/javase-jdk8-downloads.html)
- Node v9.11.2 / npm version 6.4.1
- Maven 3.6.3
- [Jaspersoft Studio 6.11.0](https://community.jaspersoft.com/project/jaspersoft-studio/releases) (optional)


### Used technologies

- Spring Boot 2.2.2
- Java 1.8
- OAuth 2
- Angular JS 1.5.8

An executable Jar can be built with:
```
mvn clean package
```
