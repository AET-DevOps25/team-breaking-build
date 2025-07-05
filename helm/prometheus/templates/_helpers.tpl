{{/*
Return the name of the chart
*/}}
{{- define "prometheus.name" -}}
{{ .Values.name | default .Chart.Name }}
{{- end }}

{{/*
Return the chart name with prefix (customizable fullname)
*/}}
{{- define "prometheus.fullname" -}}
{{- if .Values.prefix -}}
{{ .Values.prefix }}-{{ include "prometheus.name" . }}
{{- else -}}
{{ include "prometheus.name" . }}
{{- end -}}
{{- end }}

{{/*
Return the full service name
*/}}
{{- define "prometheus.serviceName" -}}
{{ include "prometheus.fullname" . }}-service
{{- end }}

{{/*
Return the full config name
*/}}
{{- define "prometheus.configName" -}}
{{ include "prometheus.fullname" . }}-config
{{- end }}


{{/*
Return the full service account name
*/}}
{{- define "prometheus.serviceAccountName" -}}
{{- include "prometheus.fullname" . }}-sa
{{- end }}

{{/*
Return the full role name
*/}}
{{- define "prometheus.roleName" -}}
{{- include "prometheus.fullname" . }}-role
{{- end }}

{{/*
Return the full role binding name
*/}}
{{- define "prometheus.roleBindingName" -}}
{{- include "prometheus.fullname" . }}-rolebinding
{{- end }}

{{/*
Return the full pvc name
*/}}
{{- define "prometheus.pvcName" -}}
{{- include "prometheus.fullname" . }}-pvc
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "prometheus.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "prometheus.labels" -}}
helm.sh/chart: {{ include "prometheus.chart" . }}
{{ include "prometheus.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "prometheus.selectorLabels" -}}
app.kubernetes.io/name: {{ include "prometheus.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- end }}
