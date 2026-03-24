# AWS Deployment Guide — OddsPulse

> All commands use `eu-west-1`. Replace `<ACCOUNT_ID>` with your 12-digit AWS account ID.

---

## 1. RDS PostgreSQL (Free Tier)

```bash
# Create a db.t3.micro PostgreSQL instance (free-tier eligible)
aws rds create-db-instance \
  --db-instance-identifier oddspulse-db \
  --db-instance-class db.t3.micro \
  --engine postgres \
  --engine-version 16.3 \
  --master-username odds_user \
  --master-user-password odds_pass \
  --allocated-storage 20 \
  --db-name oddspulse \
  --publicly-accessible \
  --region eu-west-1

# Wait until the instance is available
aws rds wait db-instance-available \
  --db-instance-identifier oddspulse-db \
  --region eu-west-1

# Get the endpoint (note this as DB_URL)
aws rds describe-db-instances \
  --db-instance-identifier oddspulse-db \
  --query "DBInstances[0].Endpoint.Address" \
  --output text \
  --region eu-west-1
```

Your `DB_URL` will be:
```
jdbc:postgresql://<endpoint>:5432/oddspulse
```

---

## 2. SQS Queue

```bash
# Create a standard queue
aws sqs create-queue \
  --queue-name odds-feed-queue \
  --region eu-west-1

# Get the queue URL (note this as SQS_QUEUE_URL)
aws sqs get-queue-url \
  --queue-name odds-feed-queue \
  --query "QueueUrl" \
  --output text \
  --region eu-west-1
```

---

## 3. S3 Bucket

```bash
# Create the bucket (globally unique name required)
aws s3api create-bucket \
  --bucket oddspulse-snapshots-<ACCOUNT_ID> \
  --region eu-west-1 \
  --create-bucket-configuration LocationConstraint=eu-west-1

# Block all public access (data is internal)
aws s3api put-public-access-block \
  --bucket oddspulse-snapshots-<ACCOUNT_ID> \
  --public-access-block-configuration \
    BlockPublicAcls=true,IgnorePublicAcls=true,BlockPublicPolicy=true,RestrictPublicBuckets=true
```

Note the bucket name as `S3_BUCKET_NAME`: `oddspulse-snapshots-<ACCOUNT_ID>`

---

## 4. IAM Role for EC2

```bash
# Create the trust policy file
cat > ec2-trust-policy.json << 'EOF'
{
  "Version": "2012-10-17",
  "Statement": [
    {
      "Effect": "Allow",
      "Principal": { "Service": "ec2.amazonaws.com" },
      "Action": "sts:AssumeRole"
    }
  ]
}
EOF

# Create the IAM role
aws iam create-role \
  --role-name oddspulse-ec2-role \
  --assume-role-policy-document file://ec2-trust-policy.json

# Attach SQS and S3 policies
aws iam attach-role-policy \
  --role-name oddspulse-ec2-role \
  --policy-arn arn:aws:iam::aws:policy/AmazonSQSFullAccess

aws iam attach-role-policy \
  --role-name oddspulse-ec2-role \
  --policy-arn arn:aws:iam::aws:policy/AmazonS3FullAccess

# Create an instance profile and add the role
aws iam create-instance-profile \
  --instance-profile-name oddspulse-ec2-profile

aws iam add-role-to-instance-profile \
  --instance-profile-name oddspulse-ec2-profile \
  --role-name oddspulse-ec2-role
```

---

## 5. ECR Repository

```bash
# Create the ECR repository
aws ecr create-repository \
  --repository-name oddspulse \
  --region eu-west-1

# Authenticate Docker with ECR
aws ecr get-login-password --region eu-west-1 | \
  docker login --username AWS --password-stdin \
  <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com

# Build, tag and push the image
docker build -t oddspulse .

docker tag oddspulse:latest \
  <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com/oddspulse:latest

docker push \
  <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com/oddspulse:latest
```

---

## 6. EC2 Instance (t2.micro)

```bash
# Launch an EC2 instance with the IAM role attached
aws ec2 run-instances \
  --image-id ami-0c38b837cd80f13bb \
  --instance-type t2.micro \
  --iam-instance-profile Name=oddspulse-ec2-profile \
  --key-name <YOUR_KEY_PAIR> \
  --security-group-ids <YOUR_SG_ID> \
  --region eu-west-1 \
  --tag-specifications 'ResourceType=instance,Tags=[{Key=Name,Value=oddspulse-server}]'
```

### SSH into the instance and set up Docker:

```bash
# Install Docker
sudo yum update -y
sudo yum install -y docker
sudo systemctl start docker
sudo systemctl enable docker
sudo usermod -aG docker ec2-user

# Re-login for group changes, then authenticate with ECR
aws ecr get-login-password --region eu-west-1 | \
  docker login --username AWS --password-stdin \
  <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com

# Pull and run the application
docker pull <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com/oddspulse:latest

docker run -d \
  --name oddspulse \
  -p 8080:8080 \
  -e DB_URL="jdbc:postgresql://<RDS_ENDPOINT>:5432/oddspulse" \
  -e DB_USERNAME="odds_user" \
  -e DB_PASSWORD="odds_pass" \
  -e SQS_QUEUE_URL="<SQS_QUEUE_URL>" \
  -e S3_BUCKET_NAME="oddspulse-snapshots-<ACCOUNT_ID>" \
  -e AWS_REGION="eu-west-1" \
  <ACCOUNT_ID>.dkr.ecr.eu-west-1.amazonaws.com/oddspulse:latest
```

### Verify:

```bash
# Check container logs
docker logs -f oddspulse

# Test health endpoint
curl http://localhost:8080/actuator/health
```

---

## Environment Variables Summary

| Variable | Source | Example |
|---|---|---|
| `DB_URL` | RDS endpoint | `jdbc:postgresql://oddspulse-db.xxx.eu-west-1.rds.amazonaws.com:5432/oddspulse` |
| `DB_USERNAME` | RDS master user | `odds_user` |
| `DB_PASSWORD` | RDS master password | `odds_pass` |
| `SQS_QUEUE_URL` | SQS console/CLI | `https://sqs.eu-west-1.amazonaws.com/<ACCOUNT_ID>/odds-feed-queue` |
| `S3_BUCKET_NAME` | S3 bucket name | `oddspulse-snapshots-<ACCOUNT_ID>` |
