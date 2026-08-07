#!/bin/sh
set -eu

cat >/tmp/stack-inicial.yaml <<'YAML'
AWSTemplateFormatVersion: "2010-09-09"
Resources:
  Fila:
    Type: AWS::SQS::Queue
    Properties:
      QueueName: learning-infra-drift
      VisibilityTimeout: 5
YAML

aws cloudformation deploy \
  --stack-name learning-infra-drift \
  --template-file /tmp/stack-inicial.yaml

