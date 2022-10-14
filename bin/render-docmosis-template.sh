
#!/bin/bash

while getopts ":t:t:" opt; do
  case $opt in
    t) TemplateName="$OPTARG"
    ;;
    \?) echo "Invalid option -$OPTARG" >&2
    exit 1
    ;;
  esac

  case $OPTARG in
    -*) echo "Option $opt needs a valid argument"
    exit 1
    ;;
  esac
done

if test -z "$TemplateName"
then
      echo "Argument t is empty"
      echo "t: template"
      exit 0
fi

if test -z "${DOCMOSIS_ACCESS_KEY}"
then
      echo "DOCMOSIS_ACCESS_KEY is empty"
      exit 0
fi

accessKey=${DOCMOSIS_ACCESS_KEY}
Uri="https://eu.dws3.docmosis.com/api/render"

renderFolder="build/renders"

entities=("Appellant|Appellant"
"Appellant|Appointee"
"Appellant|Representative"
"JointParty|JointParty"
"OtherParty|OtherParty"
"OtherParty|Appointee"
"OtherParty|Representative")

templateNameRemote="Sscs/$TemplateName"

templateJson="{
'accessKey': '$accessKey',
'templateName': '$templateNameRemote',
'outputName': '#outputName',
'data': {
	'name': 'John Smith',
	'sscs_url': 'www.gov.uk/appeal-benefit-decision',
	'generated_date': '2019-06-17',
	'address_name':'Harry Tiles',
	'letter_address_line_1': 'address line 1',
	'letter_address_line_2': 'address line 2',
	'letter_address_town': 'town',
	'letter_address_county': 'county',
	'letter_address_postcode': 'postcode',
	'benefit_name_acronym': 'PIP',
	'benefit_name_acronym_welsh': 'TAP',
	'appeal_ref': '1234567891234567',
	'benefit_full_name': 'benefit_full_name',
	'first_tier_agency_acronym': 'DWP',
	'welsh_first_tier_agency_acronym': 'GCP',
	'first_tier_agency_full_name': 'Department for Work and Pensions',
	'first_tier_agency_group':'the Child Maintenance Group',
	'phone_number': '01000000000',
	'phone_number_welsh': '02000000000',
	'hmcts2':'[userImage:hmcts.png]',
	'with_optional_the': 'the ',
	'appellants_name': 'Jane Bloggs',
	'representeeName': 'Richard Figg',
	'partyType': '#partyType',
	'entityType': '#entityType'
}
}
"

mkdir -p $renderFolder

for entity in "${entities[@]}"
do
  IFS='|' read -ra entityArr <<< "$entity"
	partyType="${entityArr[0]}"
	entityType="${entityArr[1]}"
	outputName=${TemplateName//".docx"/" - $partyType $entityType.pdf"}
	json="$templateJson"
	json=${json//"#partyType"/$partyType}
	json=${json//"#entityType"/$entityType}
	json=${json//"#outputName"/$outputName}
	echo "Rendering $outputName"
	curl -X POST -H "Content-Type:application/json" --data "${json}" -o "$renderFolder/$outputName" ${Uri}
done
