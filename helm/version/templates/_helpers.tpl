{{/*
Return the name of the chart
*/}}
{{- define "version.name" -}}
{{ .Values.name | default .Chart.Name }}
{{- end }}

{{/*
Return the chart name with prefix (customizable fullname)
*/}}
{{- define "version.fullname" -}}
{{- if .Values.prefix -}}
{{ .Values.prefix }}-{{ include "version.name" . }}
{{- else -}}
{{ include "version.name" . }}
{{- end -}}
{{- end }}

{{/*
Return the full service name
*/}}
{{- define "version.serviceName" -}}
{{ include "version.fullname" . }}-service
{{- end }}

{{/*
Return the full config name
*/}}
{{- define "version.configName" -}}
{{ include "version.fullname" . }}-config
{{- end }}

{{/*
Return the full config name
*/}}
{{- define "version.secretName" -}}
{{ include "version.fullname" . }}-secret
{{- end }}

{{/*
Return the full service account name
*/}}
{{- define "version.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- include "version.fullname" . }}-sa
{{- else }}
{{ "default" }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "version.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "version.labels" -}}
helm.sh/chart: {{ include "version.chart" . }}
{{ include "version.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "version.selectorLabels" -}}
app.kubernetes.io/name: {{ include "version.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
