# deploy command (run from mono-repo root)
gcloud run deploy alarm-system-server `
    --source . `
    --region europe-west2 `
    --env-vars-file .env `
    --use-http2