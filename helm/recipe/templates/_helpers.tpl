{{/*
Return the name of the chart
*/}}
{{- define "recipe.name" -}}
{{ .Values.name | default .Chart.Name }}
{{- end }}

{{/*
Return the chart name with prefix (customizable fullname)
*/}}
{{- define "recipe.fullname" -}}
{{- if .Values.prefix -}}
{{ .Values.prefix }}-{{ include "recipe.name" . }}
{{- else -}}
{{ include "recipe.name" . }}
{{- end -}}
{{- end }}

{{/*
Return the full service name
*/}}
{{- define "recipe.serviceName" -}}
{{ include "recipe.fullname" . }}-service
{{- end }}

{{/*
Return the full service account name
*/}}
{{- define "recipe.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- include "recipe.fullname" . }}-sa
{{- else }}
{{ "default" }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "recipe.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "recipe.labels" -}}
helm.sh/chart: {{ include "recipe.chart" . }}
{{ include "recipe.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "recipe.selectorLabels" -}}
app.kubernetes.io/name: {{ include "recipe.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
