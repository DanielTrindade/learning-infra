resource "aws_vpc" "mirante" {
  cidr_block           = "10.60.0.0/16"
  enable_dns_hostnames = true

  tags = {
    Name = "mirante"
  }
}

resource "aws_subnet" "publica_a" {
  vpc_id            = aws_vpc.mirante.id
  cidr_block        = "10.60.1.0/24"
  availability_zone = "us-east-1a"

  tags = {
    Name = "mirante-publica-a"
  }
}
