export class LoginDto {
  email: string;
  password: string;
}

export class RegisterHumanDto {
  name: string;
  email: string;
  password: string;
}

export class RegisterShelterDto {
  name: string;
  email: string;
  password: string;
  description: string;
  location: string;
  phone: string;
  verificationDocs: string[];
}

export class LinkAnimalDto {
  credentialUsername: string;
  credentialPassword: string;
}