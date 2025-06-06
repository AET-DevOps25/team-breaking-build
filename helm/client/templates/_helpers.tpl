{{/*
Return the name of the chart
*/}}
{{- define "client.name" -}}
{{ .Values.name | default .Chart.Name }}
{{- end }}

{{/*
Return the chart name with prefix (customizable fullname)
*/}}
{{- define "client.fullname" -}}
{{- if .Values.prefix -}}
{{ .Values.prefix }}-{{ include "client.name" . }}
{{- else -}}
{{ include "client.name" . }}
{{- end -}}
{{- end }}

{{/*
Return the full service name
*/}}
{{- define "client.serviceName" -}}
{{ include "client.fullname" . }}
{{- end }}

{{/*
Return the full service account name
*/}}
{{- define "client.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- include "client.fullname" . }}-sa
{{- else }}
{{ "default" }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "client.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "client.labels" -}}
helm.sh/chart: {{ include "client.chart" . }}
{{ include "client.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "client.selectorLabels" -}}
app.kubernetes.io/name: {{ include "client.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
