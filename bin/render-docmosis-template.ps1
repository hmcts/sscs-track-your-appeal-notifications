param(
     [Parameter()]
     [string]$TemplateName
 )

$accessKey = $env:DOCMOSIS_ACCESS_KEY
$Uri = "https://eu.dws3.docmosis.com/api/render"

$renderFolder = "build\renders"

$entities= (
	("Appellant","Appellant"),
	("Appellant","Appointee"),
	("Appellant","Representative"),
	("JointParty","JointParty"),
	("OtherParty","OtherParty"),
	("OtherParty","Appointee"),
	("OtherParty","Representative")
)

$templateNameRemote = "Sscs/$TemplateName"

$templateJson = @"
{
"accessKey": "$accessKey",
"templateName": "$templateNameRemote",
"outputName": "#outputName",
"data": {
	"name": "John Smith",
	"sscs_url": "www.gov.uk/appeal-benefit-decision",
	"generated_date": "2019-06-17",
	"address_name":"Harry Tiles",
	"letter_address_line_1": "address line 1",
	"letter_address_line_2": "address line 2",
	"letter_address_town": "town",
	"letter_address_county": "county",
	"letter_address_postcode": "postcode",
	"benefit_name_acronym": "PIP",
	"benefit_name_acronym_welsh": "TAP",
	"appeal_ref": "1234567891234567",
	"benefit_full_name": "benefit_full_name",
	"first_tier_agency_acronym": "DWP",
	"welsh_first_tier_agency_acronym": "GCP",
	"first_tier_agency_full_name": "Department for Work and Pensions",
	"first_tier_agency_group":"the Child Maintenance Group",
	"phone_number": "01000000000",
	"phone_number_welsh": "02000000000",
	"hmcts2":"[userImage:hmcts.png]",
	"with_optional_the": "the ",
	"appellants_name": "Jane Bloggs",
	"representeeName": "Richard Figg",
	"partyType": "#partyType",
	"entityType": "#entityType"
}
}
"@

if (!(Test-Path $renderFolder)){
New-Item -itemType Directory -Path .\$renderFolder
}

foreach ($entity in $entities) {
	$partyType = $entity[0]
	$entityType = $entity[1]
	$outputName = $TemplateName.replace('.docx'," - $partyType $entityType.pdf")
	$json = $templateJson
	$json = $json.replace('#partyType',"$partyType")
	$json = $json.replace('#entityType',"$entityType")
	$json = $json.replace('#outputName',"$outputName")
	echo "Rendering $outputName"
	Invoke-RestMethod -Uri $Uri -Method Post -ContentType "application/json" -Body $json -OutFile "$renderFolder\$outputName"
}