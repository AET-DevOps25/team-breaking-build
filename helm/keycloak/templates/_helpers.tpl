{{- /*
Return the full release name, truncated to 63 chars.
*/ -}}
{{- define "keycloak.fullname" -}}
{{- printf "%s-%s" .Release.Name .Chart.Name | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{- /*
Standard labels to apply everywhere.
*/ -}}
{{- define "keycloak.labels" -}}
app.kubernetes.io/name: {{ include "keycloak.fullname" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app.kubernetes.io/managed-by: Helm
{{- if .Values.global.security.allowInsecure }}
app.kubernetes.io/security: "insecure-allowed"
{{- end }}
{{- end -}}