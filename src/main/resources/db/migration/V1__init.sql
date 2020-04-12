create table AppRight (
   appRightId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	code varchar(255),
	description varchar(255),
	displayName varchar(255),
	primary key (appRightId)
);

create table AppUser (
   appUserId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	deleted boolean,
	lastModifier varchar(255),
	appUserName varchar(255) not null,
	appUserPassword varchar(255),
	email varchar(255),
	passwordForgotKeyHash varchar(255),
	paypalName varchar(255),
	primary key (appUserId)
);

create table AppUser_userContacts (
   AppUser_appUserId uuid not null,
	userContacts_userContactId uuid not null
);

create table AppUser_userRights (
   AppUser_appUserId uuid not null,
	userRights_appRightId uuid not null,
	primary key (AppUser_appUserId, userRights_appRightId)
);

create table BasicData (
   basicDataId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	basicDataSubType varchar(255),
	basicDataType varchar(255),
	numberValue numeric(19, 2),
	object1Class varchar(255),
	object1Id varchar(255),
	object2Class varchar(255),
	object2Id varchar(255),
	value varchar(255),
	appUser_appUserId uuid,
	primary key (basicDataId)
);

create table Billing (
   billingId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	billingStatusEnum varchar(255),
	costPayerId uuid,
	costPayerTypeEnum varchar(255),
	isNormalPayment boolean,
	sumPaid numeric(19, 2),
	sumToPay numeric(19, 2),
	primary key (billingId)
);

create table Budget (
   budgetId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	budgetRepetitionType varchar(255),
	currentSum numeric(19, 2),
	filterText varchar(255),
	lastExceeding timestamp,
	lastExceedingSum numeric(19, 2),
	lastSum numeric(19, 2),
	name varchar(255),
	paymentTypeEnum varchar(255),
	remarks varchar(255),
	specialType boolean,
	sum numeric(19, 2) not null,
	appUser_appUserId uuid,
	primary key (budgetId)
);

create table BudgetCategory (
   budgetCategoryId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	budget_budgetId uuid,
	invoiceCategory_invoiceCategoryId uuid,
	primary key (budgetCategoryId)
);

create table BudgetRecipient (
   budgetRecipientId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	payerId uuid,
	paymentPersonTypeEnum varchar(255),
	budget_budgetId uuid,
	primary key (budgetRecipientId)
);

create table BusinessPartner (
   businessPartnerId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	amountBusinessPartnerCategoryUsage int8,
	amountUsages int8,
	basicStatusEnum varchar(255),
	businessPartnerName varchar(255) not null,
	businessPartnerPublicStatus varchar(255),
	businessPartnerReceiptName varchar(255) not null,
	categoryPublicStatus varchar(255),
	appUser_appUserId uuid,
	invoiceCategory_invoiceCategoryId uuid,
	primary key (businessPartnerId)
);

create table CostDistribution (
   costDistributionId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	basicStatusEnum varchar(255),
	name varchar(255),
	primary key (costDistributionId)
);

create table CostDistributionItem (
   costDistributionItemId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	articleDTOsAsJson varchar(1000000),
	correctionStatus varchar(255),
	costDistributionItemTypeEnum varchar(255),
	costPaid numeric(19, 2),
	moneyValue numeric(19, 2),
	payerId uuid,
	paymentPersonTypeEnum varchar(255),
	position int4,
	remarks varchar(255),
	value numeric(19, 2),
	costDistribution_costDistributionId uuid,
	invoice_invoiceId uuid,
	primary key (costDistributionItemId)
);

create table CostDistributionItem_billings (
   CostDistributionItem_costDistributionItemId uuid not null,
	billings_billingId uuid not null,
	primary key (CostDistributionItem_costDistributionItemId, billings_billingId)
);

create table Invoice (
   invoiceId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	articleDTOsAsJson varchar(1000000),
	correctionStatus varchar(255),
	costPaid numeric(19, 2),
	dateOfInvoice timestamp,
	invoiceSource varchar(255),
	invoiceStatusEnum varchar(255),
	ocrFullText varchar(1000000),
	payerId uuid,
	payerTypeEnum varchar(255),
	paymentRecipientId uuid,
	paymentRecipientTypeEnum varchar(255),
	paymentTypeEnum varchar(255),
	remarks varchar(1000000),
	repetitionTypeEnum varchar(255),
	scansioResultData varchar(1000000),
	specialType boolean,
	sumOfInvoice numeric(10, 2),
	invoiceCategory_invoiceCategoryId uuid,
	invoiceImage_invoiceImageId uuid,
	primary key (invoiceId)
);

create table Invoice_billings (
   Invoice_invoiceId uuid not null,
	billings_billingId uuid not null,
	primary key (Invoice_invoiceId, billings_billingId)
);

create table InvoiceCategory (
   invoiceCategoryId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	basicStatusEnum varchar(255),
	invoiceCategoryName varchar(255),
	invoiceCategoryType varchar(255),
	appUser_appUserId uuid,
	parentInvoiceCategory_invoiceCategoryId uuid,
	primary key (invoiceCategoryId)
);

create table InvoiceCategoryKeyword (
   invoiceCategoryKeywordId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	keyword varchar(255),
	invoiceCategory_invoiceCategoryId uuid,
	primary key (invoiceCategoryKeywordId)
);

create table InvoiceFailure (
   invoiceFailureId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	invoiceFailureTypeEnum varchar(255),
	message varchar(255),
	invoice_invoiceId uuid,
	primary key (invoiceFailureId)
);

create table InvoiceImage (
   invoiceImageId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	fileName varchar(255),
	fileNameOriginal varchar(255),
	rotate boolean,
	primary key (invoiceImageId)
);

create table MobileDevice (
   mobileDeviceId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	deviceId varchar(255),
	firebaseToken varchar(255),
	appUser_appUserId uuid,
	primary key (mobileDeviceId)
);

create table Settings (
   settingsId uuid not null,
	clientSecret varchar(255),
	deleteMail boolean,
	domainUrl varchar(255),
	filesPath varchar(255),
	imapEmail varchar(255),
	imapMailServiceEnabled boolean,
	imapPassword varchar(255),
	imapPath varchar(255),
	imapServer varchar(255),
	imapUser varchar(255),
	isCustomized boolean,
	jwtStoreKey varchar(255),
	linksInMails boolean,
	pathToFirefox varchar(255),
	salt varchar(255),
	scansioAccessToken varchar(5000),
	scansioEnabled boolean,
	smtpEmail varchar(255),
	smtpMailServiceEnabled boolean,
	smtpPassword varchar(255),
	smtpServer varchar(255),
	smtpUser varchar(255),
	useFirefoxForHtmlToImage boolean,
	newestVersion varchar(255),
	lastVersionCheck timestamp,
	primary key (settingsId)
);

create table StandingOrder (
   standingOrderId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	repetitionTypeEnum varchar(255),
	startDate timestamp,
	invoiceTemplate_invoiceId uuid,
	primary key (standingOrderId)
);

create table StandingOrderItem (
   standingOrderItemId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	dateCreated timestamp,
	createdInvoice_invoiceId uuid,
	standingOrder_standingOrderId uuid,
	primary key (standingOrderItemId)
);

create table UserContact (
   userContactId uuid not null,
	createdBy varchar(255),
	createdDate timestamp,
	lastModifiedAt timestamp,
	lastModifier varchar(255),
	basicStatusEnum varchar(255),
	contactName varchar(255),
	email varchar(255),
	project boolean,
	appUser_appUserId uuid,
	appUserContact_appUserId uuid,
	primary key (userContactId)
);

alter table if exists AppUser_userContacts
   add constraint UK_cgueo70qcn46p8lyk6f48b977 unique (userContacts_userContactId);

alter table if exists AppUser_userContacts
   add constraint FKk29icdwrd3qbtixj5ju45wvj4
   foreign key (userContacts_userContactId)
   references UserContact;

alter table if exists AppUser_userContacts
   add constraint FK413f8iebuys0x3agkaf7mis0q
   foreign key (AppUser_appUserId)
   references AppUser;

alter table if exists AppUser_userRights
   add constraint FKtcsael6ukksjc150kqsue1bw3
   foreign key (userRights_appRightId)
   references AppRight;

alter table if exists AppUser_userRights
   add constraint FK74o5f13kp4su26eis52jn5qwt
   foreign key (AppUser_appUserId)
   references AppUser;

alter table if exists BasicData
   add constraint FK6e5fjqtbg0dd4tusvbfatcahn
   foreign key (appUser_appUserId)
   references AppUser;

alter table if exists Budget
   add constraint FKkompv4vxq1dw2qg85qo6l3rsd
   foreign key (appUser_appUserId)
   references AppUser;

alter table if exists BudgetCategory
   add constraint FK7lnfxdo7bg4qqxam5y2cda85d
   foreign key (budget_budgetId)
   references Budget;

alter table if exists BudgetCategory
   add constraint FKlstnydu39vhnkn0xyc92rgmwp
   foreign key (invoiceCategory_invoiceCategoryId)
   references InvoiceCategory;

alter table if exists BudgetRecipient
   add constraint FK4ci26df29l3kmlnd6chbhe27q
   foreign key (budget_budgetId)
   references Budget;

alter table if exists BusinessPartner
   add constraint FKicwd1j8chburcu88it0t4ga8v
   foreign key (appUser_appUserId)
   references AppUser;

alter table if exists BusinessPartner
   add constraint FKmqso66e240x1d8l9rix5ktkd9
   foreign key (invoiceCategory_invoiceCategoryId)
   references InvoiceCategory;

alter table if exists CostDistributionItem
   add constraint FK9918ea7fh32du7pry4sosivwm
   foreign key (costDistribution_costDistributionId)
   references CostDistribution;

alter table if exists CostDistributionItem
   add constraint FKesgo2drbtn59e9sp99ug9wk53
   foreign key (invoice_invoiceId)
   references Invoice;

alter table if exists CostDistributionItem_billings
   add constraint FKewq3dmovhpchsnw8vegykl37s
   foreign key (billings_billingId)
   references Billing;

alter table if exists CostDistributionItem_billings
   add constraint FKkk2cruuc7m538buehk8kpiwge
   foreign key (CostDistributionItem_costDistributionItemId)
   references CostDistributionItem;

alter table if exists Invoice
   add constraint FKbxxt8tm18mdsbyti4v0lvptud
   foreign key (invoiceCategory_invoiceCategoryId)
   references InvoiceCategory;

alter table if exists Invoice
   add constraint FKlc0j9185neql14ws7c2bkdwag
   foreign key (invoiceImage_invoiceImageId)
   references InvoiceImage;

alter table if exists Invoice_billings
   add constraint FKcj48s5wuvhxikdsef6mhw5fmm
   foreign key (billings_billingId)
   references Billing;

alter table if exists Invoice_billings
   add constraint FKocisaokvuikb86jx56v4dsth5
   foreign key (Invoice_invoiceId)
   references Invoice;

alter table if exists InvoiceCategory
   add constraint FKh79yav7s7hlasq2dw4owvlycx
   foreign key (appUser_appUserId)
   references AppUser;

alter table if exists InvoiceCategory
   add constraint FK3y3vpthf28y9q68olih7w1ni2
   foreign key (parentInvoiceCategory_invoiceCategoryId)
   references InvoiceCategory;

alter table if exists InvoiceCategoryKeyword
   add constraint FKrnup0n4825bbc43ckbalt7ujb
   foreign key (invoiceCategory_invoiceCategoryId)
   references InvoiceCategory;

alter table if exists InvoiceFailure
   add constraint FKqiw8i02sq4pbtismvk79opv9d
   foreign key (invoice_invoiceId)
   references Invoice;

alter table if exists MobileDevice
   add constraint FKqkofoybx2ulxgwd6l0nda2u75
   foreign key (appUser_appUserId)
   references AppUser;

alter table if exists StandingOrder
   add constraint FK4ayvf3gyqxiqptw9htpkjyk1q
   foreign key (invoiceTemplate_invoiceId)
   references Invoice;

alter table if exists StandingOrderItem
   add constraint FKks5qc3uhgvofb6hqy9phnvh2r
   foreign key (createdInvoice_invoiceId)
   references Invoice;

alter table if exists StandingOrderItem
   add constraint FKnllt4my76w7x8x6ucm7wvvhww
   foreign key (standingOrder_standingOrderId)
   references StandingOrder;

alter table if exists UserContact
   add constraint FKmrnqtuom34c59pbweh952lirw
   foreign key (appUser_appUserId)
   references AppUser;

alter table if exists UserContact
   add constraint FKtn2bibkqsrwmvb8dap2rj1daf
   foreign key (appUserContact_appUserId)
   references AppUser;