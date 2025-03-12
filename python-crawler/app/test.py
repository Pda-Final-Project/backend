from sec_edgar_api import EdgarClient

edgar = EdgarClient(user_agent="<Sample Company Name> <Admin Contact>@<Sample Company Domain>")

print(edgar.get_submissions(cik="320193"))