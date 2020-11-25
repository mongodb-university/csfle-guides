package patient

type medicalRecord struct {
	Weight        int    `bson:"weight"`
	BloodPressure string `bson:"bloodPressure"`
}

type insurance struct {
	Provider     string `bson:"provider"`
	PolicyNumber int    `bson:"policyNumber"`
}

type Patient struct {
	Name           string          `bson:"name"`
	SSN            int             `bson:"ssn"`
	BloodType      string          `bson:"bloodType"`
	medicalRecords []medicalRecord `bson:"medicalRecords"`
	insurance      insurance       `bson:"insurance"`
}

func GetExamplePatient() Patient {

	return Patient{
		Name:      "Jon Doe",
		SSN:       241014209,
		BloodType: "AB+",
		medicalRecords: []medicalRecord{{
			Weight:        180,
			BloodPressure: "120/80",
		}},
		insurance: insurance{
			Provider:     "MaestCare",
			PolicyNumber: 123142,
		},
	}
}
